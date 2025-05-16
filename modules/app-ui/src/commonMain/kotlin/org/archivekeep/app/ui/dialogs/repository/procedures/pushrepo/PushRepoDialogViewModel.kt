package org.archivekeep.app.ui.dialogs.repository.procedures.pushrepo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync.JobState
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync.State
import org.archivekeep.app.core.procedures.sync.RepoToRepoSyncService
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.utils.procedures.ProcedureExecutionState
import org.archivekeep.app.ui.dialogs.AbstractDialog
import org.archivekeep.app.ui.dialogs.repository.procedures.sync.describePreparedSyncOperation
import org.archivekeep.app.ui.domain.data.getSyncCandidates
import org.archivekeep.app.ui.utils.stickToFirstNotNullAsState
import org.archivekeep.files.procedures.sync.RelocationSyncMode
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.mapToLoadable

class PushRepoDialogViewModel(
    val scope: CoroutineScope,
    val repositoryURI: RepositoryURI,
    val storageService: StorageService,
    val repoToRepoSyncService: RepoToRepoSyncService,
    val _onClose: () -> Unit,
) : AbstractDialog.IVM {
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

    override fun onClose() {
        _onClose()
    }

    inner class RepoStatus(
        val otherRepository: StorageRepository,
    ) {
        val repoToRepoSync = repoToRepoSyncService.getRepoToRepoSync(repositoryURI, otherRepository.uri)

        val rememberedOPFlow = repoToRepoSync.currentJobFlow.stickToFirstNotNullAsState(scope)

        @OptIn(ExperimentalCoroutinesApi::class)
        val currentSyncFlow: StateFlow<Loadable<State>> =
            rememberedOPFlow
                .flatMapLatest { rememberedOP ->
                    if (rememberedOP != null) {
                        return@flatMapLatest rememberedOP.currentState.mapToLoadable().mapLoadedData {
                            it as State
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
                        is State.Prepared ->
                            describePreparedSyncOperation(it.preparedSyncProcedure)

                        is JobState ->
                            when (it.executionState) {
                                ProcedureExecutionState.NotStarted -> "Starting"
                                ProcedureExecutionState.Running -> "Running"
                                is ProcedureExecutionState.Finished -> "Finished"
                            }
                    }
                }

        fun start() {
            val currentSync = currentSyncFlow.value

            if (currentSync !is Loadable.Loaded) {
                throw RuntimeException("illegal state")
            }

            val status = currentSync.value

            if (status !is State.Prepared) {
                throw RuntimeException("illegal state")
            }

            // TODO: status.startExecution()
        }
    }
}
