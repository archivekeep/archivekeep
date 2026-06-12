package org.archivekeep.app.core.procedures.deletedcleanup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.files.procedures.deletedcleanup.DeletedFilesCleanupProcedure
import org.archivekeep.files.procedures.deletedcleanup.DeletedFilesCleanupProgress
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.procedures.ProcedureExecutionState

interface DeletedFilesCleanupProcedureSupervisor {
    val currentJobFlow: StateFlow<JobWrapper<JobState>?>

    fun prepare(): Flow<Loadable<Preparation>>

    sealed interface State

    data class JobState(
        val reindexProgress: DeletedFilesCleanupProgress,
        val log: String,
        val state: ProcedureExecutionState,
    ) : State

    data class Preparation(
        val result: DeletedFilesCleanupProcedure.PreparationResult,
        val launch: (launchOptions: DeletedFilesCleanupProcedure.LaunchOptions) -> Unit,
    ) : State
}
