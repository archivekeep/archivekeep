package org.archivekeep.app.desktop.ui.dialogs.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.operations.sync.RepoToRepoSync
import org.archivekeep.app.core.operations.sync.RepoToRepoSync.JobState
import org.archivekeep.app.desktop.utils.stickToFirstNotNullAsState
import org.archivekeep.files.operations.RelocationSyncMode
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.loading.mapToLoadable

class RepoToRepoSyncUserFlow(
    val scope: CoroutineScope,
    val sync: RepoToRepoSync,
) {
    companion object {
        val defaultRelocationSyncMode =
            RelocationSyncMode.Move(
                allowDuplicateIncrease = false,
                allowDuplicateReduction = false,
            )
    }

    data class State(
        val operation: Loadable<RepoToRepoSync.State>,
    ) {
        val isRunning = operation.mapIfLoadedOrDefault(false) { it is JobState.Running }
        val isCancelled = operation.mapIfLoadedOrDefault(false) { it is JobState.Finished && it.cancelled }
        val isCompleted = operation.mapIfLoadedOrDefault(false) { it is JobState.Finished }

        val canCancel = isRunning
        val canLaunch = operation.mapIfLoadedOrDefault(false) { it is RepoToRepoSync.State.Prepared && !it.preparedSyncOperation.isNoOp() }
    }

    val currentOperation = sync.currentJobFlow.stickToFirstNotNullAsState(scope)

    val relocationSyncModeFlow = MutableStateFlow<RelocationSyncMode>(defaultRelocationSyncMode)

    @OptIn(ExperimentalCoroutinesApi::class)
    val operationStateFlow =
        currentOperation
            .flatMapLatest {
                if (it == null) {
                    relocationSyncModeFlow.flatMapLatest { relocationSyncMode ->
                        sync.prepare(relocationSyncMode)
                    }
                } else {
                    it.currentState.map { it as RepoToRepoSync.State }.mapToLoadable()
                }
            }.stateIn(scope, SharingStarted.WhileSubscribed(), Loadable.Loading)

    val stateFlow = operationStateFlow.map { State(operation = it) }

    fun launch() {
        val stateLoadable =
            operationStateFlow.value as? Loadable.Loaded
                ?: throw IllegalStateException("Must be in prepared state")

        val state = stateLoadable.value as? RepoToRepoSync.State.Prepared ?: throw IllegalStateException("Must be in prepared state")

        state.startExecution()
    }

    fun cancel() {
        val operation = currentOperation.value ?: throw IllegalStateException("Must be launched")

        operation.cancel()
    }
}
