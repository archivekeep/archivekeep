package org.archivekeep.app.core.persistence.platform.demo

import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.app.core.utils.identifiers.TypedRepoURIData

class DemoRepositoryURIData(
    val storageID: String,
    val name: String,
) : TypedRepoURIData {
    override val storageURI = StorageURI(ID, storageID)

    override val defaultLabel = name

    fun toURI() = RepositoryURI(ID, "$storageID|$name")

    companion object {
        const val ID = "demo"

        fun fromSerialized(rawString: String): DemoRepositoryURIData {
            val parts = rawString.split("|", limit = 2)

            if (parts.size != 2) {
                throw RuntimeException("Wrong repository ID: $rawString")
            }

            return DemoRepositoryURIData(parts[0], parts[1])
        }
    }
}
