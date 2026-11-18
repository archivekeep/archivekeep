package org.archivekeep.app.core.persistence.drivers.googledrive

import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoogleDriveOAuthCredentialsTest {
    @Test
    fun publicClientCredentialsRefreshUsingTheStoredRefreshToken() {
        val receivedRequest = AtomicReference<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/token") { exchange ->
            receivedRequest.set(exchange.requestBody.use { it.readBytes().decodeToString() })
            val response = """{"access_token":"access-token","expires_in":3600,"token_type":"Bearer"}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()

        try {
            val clientId = "test-client.apps.googleusercontent.com"
            val credentialsJson =
                GoogleDriveOAuthCredentials.forRefreshToken(
                    clientId = clientId,
                    refreshToken = "test-refresh-token",
                    tokenUri = "http://127.0.0.1:${server.address.port}/token",
                )
            val credentialData = Json.parseToJsonElement(credentialsJson).jsonObject
            assertEquals("authorized_user", credentialData["type"]?.jsonPrimitive?.content)
            assertEquals(clientId, credentialData["client_id"]?.jsonPrimitive?.content)
            assertEquals("test-refresh-token", credentialData["refresh_token"]?.jsonPrimitive?.content)
            assertFalse("client_secret" in credentialData)

            val credentials = GoogleDriveOAuthCredentials.createGoogleCredentials(credentialsJson)
            assertEquals("access-token", credentials.refreshAccessToken().tokenValue)

            val decodedRequest = URLDecoder.decode(receivedRequest.get(), StandardCharsets.UTF_8)
            assertTrue(decodedRequest.contains("grant_type=refresh_token"))
            assertTrue(decodedRequest.contains("client_id=$clientId"))
            assertTrue(decodedRequest.contains("refresh_token=test-refresh-token"))
            assertFalse(decodedRequest.contains("client_secret="))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun previousAndroidAccountCredentialsRequireReauthorization() {
        val error =
            assertFailsWith<GoogleDriveAuthException> {
                GoogleDriveOAuthCredentials.requireSupported(
                    """{"type":"archivekeep_android_google_account","account_name":"account@example.com"}""",
                )
            }

        assertEquals(
            "Saved Google Drive credentials use the previous Android sign-in method. Sign in again to reconnect Google Drive.",
            error.message,
        )
    }

    @Test
    fun tokenEndpointErrorsAreReported() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/token") { exchange ->
            exchange.requestBody.close()
            val response = """{"error":"invalid_grant"}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(400, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()

        try {
            val credentials =
                GoogleDriveOAuthCredentials.createGoogleCredentials(
                    GoogleDriveOAuthCredentials.forRefreshToken(
                        clientId = "test-client.apps.googleusercontent.com",
                        refreshToken = "revoked-refresh-token",
                        tokenUri = "http://127.0.0.1:${server.address.port}/token",
                    ),
                )

            val error = assertFailsWith<IOException> { credentials.refreshAccessToken() }
            assertEquals("Google OAuth token refresh failed with HTTP 400", error.message)
        } finally {
            server.stop(0)
        }
    }
}
