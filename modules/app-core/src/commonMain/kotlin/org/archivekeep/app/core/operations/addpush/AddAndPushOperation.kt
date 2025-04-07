package org.archivekeep.app.core.operations.addpush

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.operations.OperationExecutionState
import org.archivekeep.files.operations.indexupdate.AddOperation
import org.archivekeep.files.operations.indexupdate.AddOperation.PreparationResult.Move
import org.archivekeep.files.operations.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.operations.indexupdate.IndexUpdateMoveProgress

interface AddAndPushOperation {
    val currentJobFlow: StateFlow<Job?>

    fun prepare(): Flow<State>

    interface Job {
        val addPreparationResult: AddOperation.PreparationResult
        val state: Flow<State>

        fun cancel()
    }

    sealed interface State

    data object NotReadyAddPushProcess : State

    data class PreparingAddPushProcess(
        val addPreparationProgress: AddOperation.PreparationProgress,
    ) : State

    data class ReadyAddPushProcess(
        val addPreprationResult: AddOperation.PreparationResult,
        val launch: (options: LaunchOptions) -> Unit,
    ) : State

    data class LaunchOptions(
        val filesToAdd: Set<String>,
        val movesToExecute: Set<Move>,
        val selectedDestinationRepositories: Set<RepositoryURI>,
    )

    data class JobState(
        val addPreparationResult: AddOperation.PreparationResult,
        val options: LaunchOptions,
        val addProgress: IndexUpdateAddProgress,
        val moveProgress: IndexUpdateMoveProgress,
        val pushProgress: Map<RepositoryURI, PushProgress>,
        val executionState: OperationExecutionState,
    ) : State

    data class PushProgress(
        val moved: Set<Move>,
        val added: Set<String>,
        val error: Map<String, Any>,
        val finished: Boolean,
    )
}
