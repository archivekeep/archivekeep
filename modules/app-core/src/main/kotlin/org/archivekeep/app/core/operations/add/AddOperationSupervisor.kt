package org.archivekeep.app.core.operations.add

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.utils.operations.OperationExecutionState
import org.archivekeep.files.operations.indexupdate.AddOperation
import org.archivekeep.files.operations.indexupdate.AddOperation.PreparationResult
import org.archivekeep.files.operations.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.operations.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.utils.loading.Loadable

interface AddOperationSupervisor {
    val currentJobFlow: StateFlow<Job?>

    fun prepare(): Flow<Loadable<Preparation>>

    interface Job {
        val preparationResult: PreparationResult
        val launchOptions: AddOperation.LaunchOptions

        val executionStateFlow: Flow<JobState>

        fun cancel()
    }

    sealed interface State

    data class JobState(
        val movesToExecute: Set<PreparationResult.Move>,
        val filesToAdd: Set<String>,
        val addProgress: IndexUpdateAddProgress,
        val moveProgress: IndexUpdateMoveProgress,
        val log: String,
        val state: OperationExecutionState,
    ) : State

    data class Preparation(
        val result: AddOperation.Preparation,
        val launch: (launchOptions: AddOperation.LaunchOptions) -> Unit,
    ) : State
}
