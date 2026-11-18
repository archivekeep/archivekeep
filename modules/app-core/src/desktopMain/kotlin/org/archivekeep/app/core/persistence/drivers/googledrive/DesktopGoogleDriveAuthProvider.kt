package org.archivekeep.app.core.persistence.drivers.googledrive

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
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

    override suspend fun authenticate(): String {
        val credential =
            withContext(Dispatchers.IO) {
                val httpTransport = NetHttpTransport()
                val jsonFactory = GsonFactory.getDefaultInstance()

                val localServerReceiver =
                    LocalServerReceiver
                        .Builder()
                        .setPort(0)
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

                AuthorizationCodeInstalledApp(flow, localServerReceiver)
                    .authorize("user")
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
