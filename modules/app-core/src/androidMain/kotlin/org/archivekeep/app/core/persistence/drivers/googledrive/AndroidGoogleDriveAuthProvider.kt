package org.archivekeep.app.core.persistence.drivers.googledrive

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.auth.http.HttpCredentialsAdapter

class AndroidGoogleDriveAuthProvider(
    override val clientId: String,
) : GoogleDriveAuthProvider {
    override suspend fun authenticate(onAuthUrl: ((String) -> Unit)?): String = throw GoogleDriveAuthException("Use browser sign-in for Google Drive")

    override fun createDriveClient(credentialsJson: String): Drive {
        val credentials = GoogleDriveOAuthCredentials.createGoogleCredentials(credentialsJson)

        return Drive
            .Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials),
            ).setApplicationName("ArchiveKeep")
            .build()
    }
}
