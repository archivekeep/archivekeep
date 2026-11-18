package org.archivekeep.app.core.persistence.drivers.googledrive

import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.core.utils.identifiers.TypedRepoURIData

data class GoogleDriveRepositoryURIData(
    val rootFolderId: String,
) : TypedRepoURIData {
    override val storageURI = StorageURI(ID, rootFolderId)

    override val defaultLabel = rootFolderId

    fun toURI() = RepositoryURI(ID, rootFolderId)

    companion object {
        const val ID = "google-drive"

        fun fromSerialized(rawString: String): GoogleDriveRepositoryURIData {
            if (rawString.isBlank()) {
                throw RuntimeException("Wrong repository ID: $rawString")
            }

            return GoogleDriveRepositoryURIData(rawString)
        }
    }
}
