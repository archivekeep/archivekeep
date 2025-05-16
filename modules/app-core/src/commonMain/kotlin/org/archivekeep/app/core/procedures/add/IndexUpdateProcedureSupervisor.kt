package org.archivekeep.app.core.procedures.add

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.ProcedureExecutionState
import org.archivekeep.files.procedures.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.PreparationResult
import org.archivekeep.utils.loading.Loadable

interface IndexUpdateProcedureSupervisor {
    val currentJobFlow: StateFlow<Job?>

    fun prepare(): Flow<Loadable<Preparation>>

    interface Job {
        val preparationResult: PreparationResult
        val launchOptions: IndexUpdateProcedure.LaunchOptions

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
        val state: ProcedureExecutionState,
    ) : State

    data class Preparation(
        val result: IndexUpdateProcedure.Preparation,
        val launch: (launchOptions: IndexUpdateProcedure.LaunchOptions) -> Unit,
    ) : State
}
