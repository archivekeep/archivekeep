package org.archivekeep.app.core.persistence.drivers.googledrive

import com.google.api.services.drive.DriveScopes
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Date

object GoogleDriveOAuthCredentials {
    private const val TOKEN_URI = "https://oauth2.googleapis.com/token"
    private const val LEGACY_ANDROID_ACCOUNT_TYPE = "archivekeep_android_google_account"

    fun forRefreshToken(
        clientId: String,
        refreshToken: String,
    ): String =
        forRefreshToken(
            clientId = clientId,
            refreshToken = refreshToken,
            tokenUri = TOKEN_URI,
        )

    internal fun forRefreshToken(
        clientId: String,
        refreshToken: String,
        tokenUri: String,
    ): String {
        require(clientId.isNotBlank()) { "Google OAuth client ID must not be blank" }
        require(refreshToken.isNotBlank()) { "Google OAuth refresh token must not be blank" }

        return buildJsonObject {
            put("type", "authorized_user")
            put("client_id", clientId)
            put("refresh_token", refreshToken)
            put("token_uri", tokenUri)
        }.toString()
    }

    fun requireSupported(credentialsJson: String) {
        if (credentialType(credentialsJson) == LEGACY_ANDROID_ACCOUNT_TYPE) {
            throw GoogleDriveAuthException(
                "Saved Google Drive credentials use the previous Android sign-in method. Sign in again to reconnect Google Drive.",
            )
        }
    }

    fun createGoogleCredentials(credentialsJson: String): GoogleCredentials {
        requireSupported(credentialsJson)
        val credentialData = Json.parseToJsonElement(credentialsJson).jsonObject
        if (credentialType(credentialData) == "authorized_user" && "client_secret" !in credentialData) {
            return PublicClientGoogleCredentials(
                clientId = credentialData.requiredString("client_id"),
                refreshToken = credentialData.requiredString("refresh_token"),
                tokenUri = credentialData.requiredString("token_uri"),
            )
        }

        return credentialsJson.byteInputStream().use { stream ->
            GoogleCredentials
                .fromStream(stream)
                .createScoped(listOf(DriveScopes.DRIVE))
        }
    }

    private fun credentialType(credentialsJson: String): String? = credentialType(Json.parseToJsonElement(credentialsJson).jsonObject)

    private fun credentialType(credentials: JsonObject): String? = credentials["type"]?.jsonPrimitive?.content

    private fun JsonObject.requiredString(name: String): String =
        this[name]
            ?.jsonPrimitive
            ?.content
            ?.takeIf { it.isNotBlank() }
            ?: throw GoogleDriveAuthException("Google Drive credentials have no $name")

    private class PublicClientGoogleCredentials(
        private val clientId: String,
        private val refreshToken: String,
        private val tokenUri: String,
    ) : GoogleCredentials() {
        override fun refreshAccessToken(): AccessToken {
            val connection = URL(tokenUri).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TOKEN_REQUEST_TIMEOUT_MILLIS
                connection.readTimeout = TOKEN_REQUEST_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")

                val requestBody =
                    listOf(
                        "grant_type" to "refresh_token",
                        "client_id" to clientId,
                        "refresh_token" to refreshToken,
                    ).joinToString("&") { (name, value) ->
                        "${URLEncoder.encode(name, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
                    }
                connection.outputStream.writer(Charsets.UTF_8).use { it.write(requestBody) }

                if (connection.responseCode !in 200..299) {
                    throw IOException("Google OAuth token refresh failed with HTTP ${connection.responseCode}")
                }

                val response =
                    Json
                        .parseToJsonElement(connection.inputStream.reader().use { it.readText() })
                        .jsonObject
                val accessToken = response.requiredString("access_token")
                val expirationSeconds =
                    response["expires_in"]?.jsonPrimitive?.content?.toLongOrNull()
                        ?: throw IOException("Google OAuth token response has no valid expiration")
                return AccessToken(accessToken, Date(System.currentTimeMillis() + expirationSeconds * 1000L))
            } finally {
                connection.disconnect()
            }
        }

        private companion object {
            const val TOKEN_REQUEST_TIMEOUT_MILLIS = 30_000
        }
    }
}
