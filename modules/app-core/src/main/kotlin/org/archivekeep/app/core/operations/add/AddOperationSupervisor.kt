package org.archivekeep.app.core.operations.add

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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

        val executionStateFlow: Flow<ExecutionState.Running>

        fun cancel()
    }

    sealed interface State

    sealed interface ExecutionState : State {
        data object NotRunning : ExecutionState

        data class Running(
            val movesToExecute: Set<PreparationResult.Move>,
            val filesToAdd: Set<String>,
            val addProgress: IndexUpdateAddProgress,
            val moveProgress: IndexUpdateMoveProgress,
            val log: String,
        ) : ExecutionState {
            val finished = addProgress.finished && moveProgress.finished
        }
    }

    data class Preparation(
        val result: AddOperation.Preparation,
        val launch: (launchOptions: AddOperation.LaunchOptions) -> Unit,
    ) : State
}
