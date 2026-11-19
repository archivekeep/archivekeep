package org.archivekeep.app.core.persistence.drivers.googledrive

import com.google.api.services.drive.Drive

interface GoogleDriveAuthProvider {
    val clientId: String

    val isSupported: Boolean
        get() = true

    suspend fun authenticate(onAuthUrl: ((String) -> Unit)? = null): String

    fun createDriveClient(credentialsJson: String): Drive
}
