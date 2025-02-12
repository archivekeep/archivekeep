package org.archivekeep.app.core.persistence.registry

import kotlinx.serialization.Serializable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.RepositoryMetadata

@Serializable
data class RegisteredRepository(
    val uri: RepositoryURI,
    val label: String? = null,
    val rememberedMetadata: RepositoryMetadata? = null,
) {
    val storageURI = uri.typedRepoURIData.storageURI

    val displayLabel = label ?: uri.typedRepoURIData.defaultLabel
}
