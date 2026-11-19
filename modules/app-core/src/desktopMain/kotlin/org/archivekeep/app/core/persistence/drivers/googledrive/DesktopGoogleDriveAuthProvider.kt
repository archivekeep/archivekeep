package org.archivekeep.app.core.persistence.drivers.googledrive

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Desktop.Action
import java.io.IOException
import java.net.URI
import java.util.Collections

class DesktopGoogleDriveAuthProvider(
    private val oauthConfig: GoogleDriveOAuthConfig,
    private val applicationName: String = "ArchiveKeep",
) : GoogleDriveAuthProvider {
    override val clientId: String
        get() = oauthConfig.clientId

    override fun createDriveClient(credentialsJson: String): Drive {
        val credentials = GoogleDriveOAuthCredentials.createGoogleCredentials(credentialsJson)

        return Drive
            .Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials),
            ).setApplicationName(applicationName)
            .build()
    }

    override suspend fun authenticate(onAuthUrl: ((String) -> Unit)?): String {
        val credential =
            withContext(Dispatchers.IO) {
                val httpTransport = NetHttpTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()

                val localServerReceiver =
                    LocalServerReceiver
                        .Builder()
                        .setPort(-1)
                        .build()

                val flow =
                    GoogleAuthorizationCodeFlow
                        .Builder(
                            httpTransport,
                            jsonFactory,
                            oauthConfig.clientId,
                            oauthConfig.clientSecret,
                            Collections.singletonList(DriveScopes.DRIVE),
                        ).setDataStoreFactory(MemoryDataStoreFactory())
                        .setAccessType("offline")
                        .setApprovalPrompt("force")
                        .build()

                // Start the local server by getting the redirect URI
                val redirectUri = localServerReceiver.getRedirectUri()

                // Build the authorization URL
                val authorizationUrl =
                    flow
                        .newAuthorizationUrl()
                        .setRedirectUri(redirectUri)
                        .build()
                        .toString()

                // Try to open the browser
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
                        Desktop.getDesktop().browse(URI.create(authorizationUrl))
                    } else {
                        onAuthUrl?.invoke(authorizationUrl)
                    }
                } catch (e: IOException) {
                    onAuthUrl?.invoke(authorizationUrl)
                } catch (e: InternalError) {
                    onAuthUrl?.invoke(authorizationUrl)
                }

                // Wait for the authorization code — the server keeps running regardless of whether
                // the browser opened successfully. The user can manually paste the URL into their
                // browser, and the callback will still be received.
                val code = localServerReceiver.waitForCode()

                // Exchange the authorization code for tokens
                val tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute()

                // Store and return the credential
                flow.createAndStoreCredential(tokenResponse, "user").also { credential ->
                    localServerReceiver.stop()
                    credential
                }
            }

        val refreshToken =
            credential.refreshToken
                ?: throw GoogleDriveAuthException("No refresh token received from Google")

        return buildAuthorizedUserCredentialsJson(
            clientId = oauthConfig.clientId,
            clientSecret = oauthConfig.clientSecret,
            refreshToken = refreshToken,
            accessToken = credential.accessToken,
        )
    }

    private fun buildAuthorizedUserCredentialsJson(
        clientId: String,
        clientSecret: String,
        refreshToken: String,
        accessToken: String?,
    ): String {
        val gson = GsonBuilder().create()
        val map =
            mutableMapOf<String, String>(
                "type" to "authorized_user",
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "refresh_token" to refreshToken,
                "token_uri" to "https://oauth2.googleapis.com/token",
            )
        if (accessToken != null) {
            map["access_token"] = accessToken
        }
        return gson.toJson(map)
    }
}
