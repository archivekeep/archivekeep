package org.archivekeep.app.ui.views.home.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.domain.storages.StoragePartiallyResolved
import org.archivekeep.app.core.procedures.sync.RepoToRepoSyncService
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.safeCombine

class HomeStorageUiState(
    scope: CoroutineScope,
    val repoToRepoSyncService: RepoToRepoSyncService,
    val storage: StoragePartiallyResolved,
    val reference: StorageNamedReference = storage.namedReference,
    val name: String? = storage.knownStorage.registeredStorage?.label,
    val otherRepositoriesInThisStorage: List<RepositoryItemModel>,
) {
    data class ResolvedState(
        val resolvedRepositories: Loadable<List<RepositoryItemUiState>>,
        val isConnected: Boolean,
    ) {
        val canPushAny = resolvedRepositories.mapIfLoadedOrDefault(false) { it.any { it.canPush } }
        val canPullAny = resolvedRepositories.mapIfLoadedOrDefault(false) { it.any { it.canPull } }
    }

    val secondaryRepositories: StateFlow<List<RepositoryItemUiState>> =
        safeCombine(otherRepositoriesInThisStorage.map { it.stateFlow(scope, repoToRepoSyncService) }) {
            it.toList()
        }.stateIn(scope, SharingStarted.Lazily, emptyList())

    val stateFlow: StateFlow<ResolvedState> =
        combine(
            secondaryRepositories,
            storage.state,
        ) { secondaryRepositories, storageState ->
            ResolvedState(
                Loadable.Loaded(secondaryRepositories.toList()),
                storageState.mapIfLoadedOrDefault(false) { it.isConnected },
            )
        }.stateIn(scope, SharingStarted.Lazily, ResolvedState(Loadable.Loading, false))
}
