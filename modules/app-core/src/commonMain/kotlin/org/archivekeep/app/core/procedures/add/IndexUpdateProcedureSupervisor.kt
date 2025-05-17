package org.archivekeep.app.core.procedures.add

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.files.procedures.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.procedures.ProcedureExecutionState

interface IndexUpdateProcedureSupervisor {
    val currentJobFlow: StateFlow<JobWrapper<JobState>?>

    fun prepare(): Flow<Loadable<Preparation>>

    sealed interface State

    data class JobState(
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
