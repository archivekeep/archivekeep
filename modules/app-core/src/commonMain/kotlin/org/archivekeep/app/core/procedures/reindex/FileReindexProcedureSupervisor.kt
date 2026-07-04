package org.archivekeep.app.core.procedures.reindex

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.BaseJobState
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.files.procedures.reindex.FileReindexProcedure
import org.archivekeep.files.procedures.reindex.FileReindexProgress
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.procedures.ProcedureExecutionState

interface FileReindexProcedureSupervisor {
    val currentJobFlow: StateFlow<JobWrapper<JobState>?>

    fun prepare(): Flow<Loadable<Preparation>>

    sealed interface State

    data class JobState(
        val reindexProgress: FileReindexProgress,
        override val progressLog: StateFlow<List<String>>,
        override val executionState: ProcedureExecutionState,
    ) : State,
        BaseJobState

    data class Preparation(
        val result: FileReindexProcedure.PreparationResult,
        val launch: (launchOptions: FileReindexProcedure.LaunchOptions) -> Unit,
    ) : State
}
