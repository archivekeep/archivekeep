package org.archivekeep.app.core.domain.archives

import org.archivekeep.app.core.domain.repositories.ResolvedRepositoryState
import org.archivekeep.app.core.domain.storages.StoragePartiallyResolved
import org.archivekeep.files.api.repository.RepositoryAssociationGroupId

data class AssociatedArchive(
    // TODO: tear apart unassociated archives (repositories without association id)
    val associationId: RepositoryAssociationGroupId?,
    val repositories: List<Pair<StoragePartiallyResolved, ResolvedRepositoryState>>,
) {
    val label = repositories.first().second.displayName

    val primaryRepository: Pair<StoragePartiallyResolved, ResolvedRepositoryState>?
        get() =
            repositories
                .firstOrNull { (storage, _) ->
                    storage.isLocal
                }
}
