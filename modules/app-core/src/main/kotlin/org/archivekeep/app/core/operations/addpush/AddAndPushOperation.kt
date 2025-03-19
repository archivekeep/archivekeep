package org.archivekeep.app.core.operations.addpush

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.operations.AddOperation
import org.archivekeep.files.operations.AddOperation.PreparationResult.Move

interface AddAndPushOperation {
    val currentJobFlow: StateFlow<Job?>

    fun prepare(): Flow<State>

    interface Job {
        val addPreparationResult: AddOperation.PreparationResult
        val state: StateFlow<State>

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

    data class LaunchedAddPushProcess(
        val addPreparationResult: AddOperation.PreparationResult,
        val options: LaunchOptions,
        val addProgress: AddProgress,
        val moveProgress: MoveProgress,
        val pushProgress: Map<RepositoryURI, PushProgress>,
        val finished: Boolean,
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

    data class PushProgress(
        val moved: Set<Move>,
        val added: Set<String>,
        val error: Map<String, Any>,
        val finished: Boolean,
    )
}
