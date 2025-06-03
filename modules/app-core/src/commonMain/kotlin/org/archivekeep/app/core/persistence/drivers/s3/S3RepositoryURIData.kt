package org.archivekeep.app.core.persistence.drivers.s3

import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.core.utils.identifiers.TypedRepoURIData

data class S3RepositoryURIData(
    val endpoint: String,
    val bucket: String,
) : TypedRepoURIData {
    override val storageURI = StorageURI("s3", endpoint)

    override val defaultLabel = bucket

    fun serialized(): String = "$endpoint|$bucket"

    companion object {
        fun fromSerialized(rawString: String): S3RepositoryURIData {
            val parts = rawString.split("|", limit = 2)

            if (parts.size != 2) {
                throw RuntimeException("Wrong repository ID: $rawString")
            }

            return S3RepositoryURIData(parts[0], parts[1])
        }
    }
}
