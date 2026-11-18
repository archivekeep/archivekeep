package org.archivekeep.app.core.persistence.platform.demo

import com.google.api.services.drive.Drive
import org.archivekeep.app.core.persistence.drivers.googledrive.GoogleDriveAuthException
import org.archivekeep.app.core.persistence.drivers.googledrive.GoogleDriveAuthProvider

object UnsupportedGoogleDriveAuthProvider : GoogleDriveAuthProvider {
    override val clientId: String = ""
    override val isSupported: Boolean = false

    override suspend fun authenticate(): String = throw GoogleDriveAuthException("Google Drive is not supported in this environment")

    override fun createDriveClient(credentialsJson: String): Drive = throw GoogleDriveAuthException("Google Drive is not supported in this environment")
}
