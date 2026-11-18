package org.archivekeep.files.driver.googledrive

import com.google.api.services.drive.model.File

internal data class GoogleDriveFile(
    val id: String,
    val name: String,
    val mimeType: String?,
    val size: Long?,
    val sha256Checksum: String?,
) {
    val isFolder: Boolean
        get() = mimeType == GOOGLE_DRIVE_FOLDER_MIME_TYPE

    val isGoogleAppDocument: Boolean
        get() = mimeType?.startsWith(GOOGLE_APPS_MIME_TYPE) == true

    fun isMetadataFolder(metadataFolderName: String) = isFolder && name == metadataFolderName

    private companion object {
        const val GOOGLE_DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        const val GOOGLE_APPS_MIME_TYPE = "application/vnd.google-apps."
    }
}

internal fun File.toGoogleDriveFile() =
    GoogleDriveFile(
        id = id,
        name = name,
        mimeType = mimeType,
        size = getSize(),
        sha256Checksum = sha256Checksum,
    )
