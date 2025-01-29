package org.archivekeep.app.desktop.ui.dialogs.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.operations.derived.PreparedRunningOrCompletedSync
import org.archivekeep.app.core.operations.derived.RepoToRepoSync
import org.archivekeep.app.core.operations.derived.SyncOperationExecution
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.generics.mapToLoadable
import org.archivekeep.app.desktop.utils.stickToFirstNotNullAsState
import org.archivekeep.core.operations.RelocationSyncMode
import org.archivekeep.utils.Loadable
import org.archivekeep.utils.mapIfLoadedOrDefault

class RepoToRepoSyncUserFlow(
    val scope: CoroutineScope,
    val sync: RepoToRepoSync,
) {
    data class State(
        val operation: Loadable<PreparedRunningOrCompletedSync>,
    ) {
        val isRunning = operation.mapIfLoadedOrDefault(false) { it is SyncOperationExecution.Running }
        val isCancelled = operation.mapIfLoadedOrDefault(false) { it is SyncOperationExecution.Finished && it.cancelled }
        val isCompleted = operation.mapIfLoadedOrDefault(false) { it is SyncOperationExecution.Finished }

        val canCancel = isRunning
        val canLaunch = operation.mapIfLoadedOrDefault(false) { it is SyncOperationExecution.Prepared }
    }

    val currentOperation = sync.currentlyRunningOperationFlow.stickToFirstNotNullAsState(scope)

    val relocationSyncModeFlow =
        MutableStateFlow(
            RelocationSyncMode.Move(
                false,
                false,
            ),
        )

    val operationStateFlow =
        currentOperation
            .flatMapLatest {
                if (it == null) {
                    relocationSyncModeFlow.flatMapLatest { relocationSyncMode ->
                        sync.prepare(relocationSyncMode)
                    }
                } else {
                    it.currentState.map { it as PreparedRunningOrCompletedSync }.mapToLoadable()
                }
            }.stateIn(scope, SharingStarted.WhileSubscribed(), Loadable.Loading)

    val stateFlow =
        operationStateFlow.map {
            State(
                operation = it.mapLoadedData { it as PreparedRunningOrCompletedSync },
            )
        }

    fun launch() {
        val stateLoadable =
            operationStateFlow.value as? Loadable.Loaded
                ?: throw IllegalStateException("Must be in prepared state")

        val state = stateLoadable.value as? SyncOperationExecution.Prepared ?: throw IllegalStateException("Must be in prepared state")

        state.startExecution()
    }

    fun cancel() {
        val operation = currentOperation.value ?: throw IllegalStateException("Must be launched")

        operation.cancel()
    }
}
