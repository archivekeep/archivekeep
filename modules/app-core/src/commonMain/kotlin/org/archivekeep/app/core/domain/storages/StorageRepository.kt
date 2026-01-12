package org.archivekeep.app.core.domain.storages

import org.archivekeep.app.core.domain.repositories.ResolvedRepositoryState
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.RepositoryAssociationGroupId

data class StorageRepository(
    val storage: StorageNamedReference,
    val uri: RepositoryURI,
    val repositoryState: ResolvedRepositoryState,
) {
    val displayName: String
        get() = repositoryState.displayName

    val associationId: RepositoryAssociationGroupId?
        get() = repositoryState.associationId
}
