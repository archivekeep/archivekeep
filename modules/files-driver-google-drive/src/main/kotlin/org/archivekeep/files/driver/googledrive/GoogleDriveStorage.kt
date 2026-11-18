package org.archivekeep.files.driver.googledrive

import com.google.api.client.http.InputStreamContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.InputStream

internal class GoogleDriveStorage(
    private val drive: Drive,
) {
    fun getFile(fileId: String): GoogleDriveFile =
        drive
            .files()
            .get(fileId)
            .setSupportsAllDrives(true)
            .setFields(FILE_FIELDS)
            .execute()
            .toGoogleDriveFile()

    fun listChildren(parentFolderId: String): List<GoogleDriveFile> {
        val result = mutableListOf<GoogleDriveFile>()
        var pageToken: String? = null

        do {
            val request =
                drive
                    .files()
                    .list()
                    .setQ("'${parentFolderId.escapeQueryValue()}' in parents and trashed = false")
                    .setCorpora("allDrives")
                    .setPageSize(1000)
                    .setSupportsAllDrives(true)
                    .setIncludeItemsFromAllDrives(true)
                    .setFields("nextPageToken, files($FILE_FIELDS)")

            if (pageToken != null) {
                request.setPageToken(pageToken)
            }

            val response = request.execute()
            result += response.files.orEmpty().map { it.toGoogleDriveFile() }
            pageToken = response.nextPageToken
        } while (pageToken != null)

        return result
    }

    fun createFolder(
        parentFolderId: String,
        name: String,
    ): GoogleDriveFile =
        drive
            .files()
            .create(
                File()
                    .setName(name)
                    .setMimeType(GOOGLE_DRIVE_FOLDER_MIME_TYPE)
                    .setParents(listOf(parentFolderId)),
            ).setSupportsAllDrives(true)
            .setFields(FILE_FIELDS)
            .execute()
            .toGoogleDriveFile()

    fun ensureFolderPath(
        parentFolderId: String,
        path: String,
    ): String {
        val parts = path.split('/').filter { it.isNotBlank() }
        var currentFolderId = parentFolderId

        for (part in parts) {
            val children = listChildren(currentFolderId)
            val existingFolder = children.find { it.name == part && it.isFolder }
            currentFolderId =
                existingFolder?.id
                    ?: drive
                        .files()
                        .create(
                            File()
                                .setName(part)
                                .setMimeType(GOOGLE_DRIVE_FOLDER_MIME_TYPE)
                                .setParents(listOf(currentFolderId)),
                        ).setSupportsAllDrives(true)
                        .setFields("id")
                        .execute()
                        .id ?: throw IllegalStateException("Created folder but no ID returned")
        }

        return currentFolderId
    }

    fun uploadFile(
        parentFolderId: String,
        name: String,
        content: InputStream,
        contentLength: Long,
        existingFileId: String? = null,
    ): GoogleDriveFile {
        val metadata = File().setName(name)
        val mediaContent = InputStreamContent(BINARY_MIME_TYPE, content).apply { setLength(contentLength) }
        val storedFile =
            if (existingFileId == null) {
                drive
                    .files()
                    .create(metadata.setParents(listOf(parentFolderId)), mediaContent)
                    .setSupportsAllDrives(true)
                    .setFields(FILE_FIELDS)
                    .execute()
            } else {
                drive
                    .files()
                    .update(existingFileId, metadata, mediaContent)
                    .setSupportsAllDrives(true)
                    .setFields(FILE_FIELDS)
                    .execute()
            }

        return storedFile.toGoogleDriveFile()
    }

    fun downloadFile(fileId: String): InputStream =
        drive
            .files()
            .get(fileId)
            .setSupportsAllDrives(true)
            .executeMediaAsInputStream()

    fun renameFile(
        fileId: String,
        name: String,
    ): GoogleDriveFile =
        drive
            .files()
            .update(fileId, File().setName(name))
            .setSupportsAllDrives(true)
            .setFields(FILE_FIELDS)
            .execute()
            .toGoogleDriveFile()

    fun moveFile(
        fileId: String,
        newParentFolderId: String,
    ): GoogleDriveFile =
        drive
            .files()
            .update(fileId, File())
            .setAddParents(newParentFolderId)
            .setRemoveParents(null)
            .setSupportsAllDrives(true)
            .setFields(FILE_FIELDS)
            .execute()
            .toGoogleDriveFile()

    fun deleteFile(fileId: String) {
        drive
            .files()
            .delete(fileId)
            .setSupportsAllDrives(true)
            .execute()
    }

    private fun String.escapeQueryValue() = replace("\\", "\\\\").replace("'", "\\'")

    private companion object {
        const val FILE_FIELDS = "id, name, mimeType, size, sha256Checksum"
        const val BINARY_MIME_TYPE = "application/octet-stream"
        const val GOOGLE_DRIVE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
    }
}
