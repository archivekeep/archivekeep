package org.archivekeep.app.core.operations.add

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.files.operations.AddOperation
import org.archivekeep.files.operations.AddOperation.PreparationResult
import org.archivekeep.files.operations.AddOperation.PreparationResult.Move
import org.archivekeep.utils.loading.Loadable

interface AddOperationSupervisor {
    val currentJobFlow: StateFlow<Job?>

    fun prepare(): Flow<Loadable<Prepared>>

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
            val addProgress: AddProgress,
            val moveProgress: MoveProgress,
            val log: String,
        ) : ExecutionState {
            val finished = addProgress.finished && moveProgress.finished
        }
    }

    data class Prepared(
        val result: PreparationResult,
        val launch: (launchOptions: AddOperation.LaunchOptions) -> Unit,
    ) : State

    data class AddProgress(
        val added: Set<String>,
        val error: Map<String, Any>,
        val finished: Boolean,
    )

    data class MoveProgress(
        val moved: Set<Move>,
        val error: Map<Move, Any>,
        val finished: Boolean,
    )
}
