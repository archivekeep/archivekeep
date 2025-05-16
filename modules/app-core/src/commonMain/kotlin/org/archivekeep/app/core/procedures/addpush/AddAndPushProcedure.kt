package org.archivekeep.app.core.procedures.addpush

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.ProcedureExecutionState
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.procedures.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.PreparationResult.Move

interface AddAndPushProcedure {
    val currentJobFlow: StateFlow<Job?>

    fun prepare(): Flow<State>

    interface Job {
        val addPreparationResult: IndexUpdateProcedure.PreparationResult
        val state: Flow<State>

        fun cancel()
    }

    sealed interface State

    data object NotReadyAddPushProcess : State

    data class PreparingAddPushProcess(
        val addPreparationProgress: IndexUpdateProcedure.PreparationProgress,
    ) : State

    data class ReadyAddPushProcess(
        val addPreprationResult: IndexUpdateProcedure.PreparationResult,
        val launch: (options: LaunchOptions) -> Unit,
    ) : State

    data class LaunchOptions(
        val filesToAdd: Set<String>,
        val movesToExecute: Set<Move>,
        val selectedDestinationRepositories: Set<RepositoryURI>,
    )

    data class JobState(
        val addPreparationResult: IndexUpdateProcedure.PreparationResult,
        val options: LaunchOptions,
        val addProgress: IndexUpdateAddProgress,
        val moveProgress: IndexUpdateMoveProgress,
        val pushProgress: Map<RepositoryURI, PushProgress>,
        val executionState: ProcedureExecutionState,
    ) : State

    data class PushProgress(
        val moved: Set<Move>,
        val added: Set<String>,
        val error: Map<String, Any>,
        val finished: Boolean,
    )
}
