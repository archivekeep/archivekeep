package org.archivekeep.app.ui.dialogs.repository.procedures.sync

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync.JobState
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState
import org.archivekeep.app.ui.components.feature.dialogs.operations.toDialogOperationControlState
import org.archivekeep.app.ui.utils.stickToFirstNotNullAsState
import org.archivekeep.files.procedures.sync.RelocationSyncMode
import org.archivekeep.files.procedures.sync.SyncOperation
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.loading.mapToLoadable

class RepoToRepoSyncUserFlow(
    val scope: CoroutineScope,
    val sync: RepoToRepoSync,
) {
    companion object {
        val relocationSyncMode = RelocationSyncMode.Move(allowDuplicateIncrease = true, allowDuplicateReduction = true)
    }

    data class State(
        val operation: Loadable<RepoToRepoSync.State>,
        val selectedOperations: MutableState<Set<SyncOperation>>,
    ) {
        fun control(
            onLaunch: () -> Unit,
            onCancel: () -> Unit,
            onClose: () -> Unit,
        ): DialogOperationControlState =
            operation.mapIfLoadedOrDefault(
                DialogOperationControlState.NotRunning(onLaunch = {}, onClose = onClose, canLaunch = false),
            ) { operationState ->
                when (operationState) {
                    is RepoToRepoSync.State.Prepared ->
                        DialogOperationControlState.NotRunning(
                            onLaunch,
                            onClose,
                            canLaunch = !operationState.preparedSyncProcedure.isNoOp(),
                        )
                    is JobState ->
                        operationState.executionState.toDialogOperationControlState(
                            onCancel = onCancel,
                            onHide = onClose,
                            onClose = onClose,
                        )
                }
            }
    }

    val currentOperationFlow = sync.currentJobFlow.stickToFirstNotNullAsState(scope)

    val selectedOperations = mutableStateOf<Set<SyncOperation>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val operationStateFlow =
        currentOperationFlow
            .flatMapLatest {
                if (it == null) {
                    sync.prepare(relocationSyncMode)
                } else {
                    it.state.mapToLoadable()
                }
            }.stateIn(scope, SharingStarted.WhileSubscribed(), Loadable.Loading)

    val stateFlow = operationStateFlow.map { State(operation = it, selectedOperations) }

    fun launch() {
        val stateLoadable =
            operationStateFlow.value as? Loadable.Loaded
                ?: throw IllegalStateException("Must be in prepared state")

        val state = stateLoadable.value as? RepoToRepoSync.State.Prepared ?: throw IllegalStateException("Must be in prepared state")

        state.startExecution(selectedOperations.value)
    }

    fun cancel() {
        val operation = currentOperationFlow.value ?: throw IllegalStateException("Must be launched")

        operation.cancel()
    }
}
