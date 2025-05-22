package org.archivekeep.app.core.procedures.addpush

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.procedures.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.PreparationResult.Move
import org.archivekeep.utils.procedures.ProcedureExecutionState
import org.archivekeep.utils.procedures.operations.OperationProgress

interface AddAndPushProcedure {
    val currentJobFlow: StateFlow<JobWrapper<JobState>?>

    fun prepare(): Flow<State>

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
        val addProgress: IndexUpdateAddProgress,
        val moveProgress: IndexUpdateMoveProgress,
        val pushProgress: Map<RepositoryURI, PushProgress>,
        val executionState: ProcedureExecutionState,
        val inProgressOperationsProgress: List<OperationProgress>,
    ) : State

    data class PushProgress(
        val moved: Set<Move>,
        val added: Set<String>,
        val error: Map<String, Any>,
        val finished: Boolean,
    )
}
