package org.archivekeep.app.core.persistence.drivers.filesystem

import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.core.utils.identifiers.TypedRepoURIData

data class FileSystemRepositoryURIData(
    val fsUUID: String,
    val pathInFS: String,
) : TypedRepoURIData {
    override val storageURI = StorageURI("filesystem", fsUUID)

    override val defaultLabel = pathInFS.split("/").last()

    fun serialized(): String = "$fsUUID|$pathInFS"

    companion object {
        fun fromSerialized(rawString: String): FileSystemRepositoryURIData {
            val parts = rawString.split("|", limit = 2)

            if (parts.size != 2) {
                throw RuntimeException("Wrong repository ID: $rawString")
            }

            return FileSystemRepositoryURIData(parts[0], parts[1])
        }
    }
}
