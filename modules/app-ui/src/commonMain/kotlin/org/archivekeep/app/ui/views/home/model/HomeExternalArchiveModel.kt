package org.archivekeep.app.ui.views.home.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.domain.archives.AssociatedArchive
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.domain.storages.needsUnlock
import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.safeCombine

class HomeExternalArchiveModel(
    val scope: CoroutineScope,
    val archive: AssociatedArchive,
    val displayName: String,
    val otherRepositories: List<OtherRepositoryDetails>,
) {
    val isAssociated = archive.associationId != null

    class OtherRepositoryDetails(
        val reference: NamedRepositoryReference,
        val storageReference: StorageNamedReference,
        val repository: Repository,
    )

    val state =
        safeCombine(
            otherRepositories
                .map { repo ->
                    combine(
                        repo.repository.optionalAccessorFlow,
                        repo.repository.localRepoStatus,
                    ) { accessor, localRepoStatus ->
                        HomeExternalArchiveRepositoryUiState(
                            uri = repo.reference.uri,
                            name =
                                repo.storageReference.displayName + (
                                    if (repo.reference.displayName != displayName) {
                                        " (${repo.reference.displayName})"
                                    } else {
                                        ""
                                    }
                                ),
                            statusText = OptionalLoadable.LoadedAvailable(""), // TODO: implement status text
                            isLoading = false, // TODO: implement loading indication
                            needsUnlock = accessor.needsUnlock(),
                            repositoryOperationalState =
                                RepositoryOperationalState(
                                    accessor,
                                    isAssociated = isAssociated,
                                    localRepoStatus.mapLoadedData { it.summary },
                                ),
                        )
                    }
                },
        ) {
            HomeExternalArchiveUiState(
                it.toList(),
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(), HomeExternalArchiveUiState(emptyList()))
}
