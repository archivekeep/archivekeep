package org.archivekeep.app.core.domain.archives

import org.archivekeep.app.core.domain.repositories.ResolvedRepositoryState
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.core.RepositoryAssociationGroupId

data class AssociatedArchive(
    // TODO: tear apart unassociated archives (repositories without association id)
    val associationId: RepositoryAssociationGroupId?,
    val repositories: List<Pair<Storage, ResolvedRepositoryState>>,
) {
    val label = repositories.first().second.displayName

    val primaryRepository: Pair<Storage, ResolvedRepositoryState>?
        get() =
            repositories
                .firstOrNull { (storage, _) ->
                    storage.isLocal
                }
}
