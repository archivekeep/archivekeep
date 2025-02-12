package org.archivekeep.app.desktop.ui.dialogs.pushrepo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.derived.PreparedRunningOrCompletedSync
import org.archivekeep.app.core.operations.derived.SyncOperationExecution
import org.archivekeep.app.core.operations.derived.SyncService
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.generics.mapToLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.data.getSyncCandidates
import org.archivekeep.app.desktop.ui.dialogs.sync.describePreparedSyncOperation
import org.archivekeep.app.desktop.utils.stickToFirstNotNullAsState
import org.archivekeep.files.operations.RelocationSyncMode
import org.archivekeep.utils.Loadable

class PushRepoDialogViewModel(
    val scope: CoroutineScope,
    val repositoryURI: RepositoryURI,
    val storageService: StorageService,
    val syncService: SyncService,
) {
    val relocationSyncModeFlow =
        MutableStateFlow<RelocationSyncMode>(
            RelocationSyncMode.Move(
                false,
                false,
            ),
        )

    val repoName = storageService.repository(repositoryURI).map { it.repositoryState.displayName }

    val otherRepos =
        getSyncCandidates(storageService, repositoryURI)
            .map { repositories ->
                repositories.map { repository ->
                    RepoStatus(
                        otherRepository = repository,
                    )
                }
            }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun startAllSync() {
        otherRepos.value.forEach {
            it.start()
        }
    }

    inner class RepoStatus(
        val otherRepository: StorageRepository,
    ) {
        val repoToRepoSync = syncService.getRepoToRepoSync(repositoryURI, otherRepository.uri)

        val rememberedOPFlow = repoToRepoSync.currentlyRunningOperationFlow.stickToFirstNotNullAsState(scope)

        val currentSyncFlow: StateFlow<Loadable<PreparedRunningOrCompletedSync>> =
            rememberedOPFlow
                .flatMapLatest { rememberedOP ->
                    if (rememberedOP != null) {
                        return@flatMapLatest rememberedOP.currentState.mapToLoadable().mapLoadedData {
                            it as PreparedRunningOrCompletedSync
                        }
                    }

                    relocationSyncModeFlow
                        .flatMapLatest { relocationSyncMode ->
                            repoToRepoSync.prepare(relocationSyncMode)
                        }
                }.stateIn(scope, SharingStarted.Eagerly, Loadable.Loading)

        val statusText =
            currentSyncFlow
                .mapLoadedData {
                    when (it) {
                        is SyncOperationExecution.Prepared ->
                            describePreparedSyncOperation(it.preparedSyncOperation)

                        is SyncOperationExecution.Running ->
                            "Running"

                        is SyncOperationExecution.Finished ->
                            "Completed"
                    }
                }

        fun start() {
            val currentSync = currentSyncFlow.value

            if (currentSync !is Loadable.Loaded) {
                throw RuntimeException("illegal state")
            }

            val status = currentSync.value

            if (status !is SyncOperationExecution.Prepared) {
                throw RuntimeException("illegal state")
            }

            status.startExecution()
        }
    }
}
