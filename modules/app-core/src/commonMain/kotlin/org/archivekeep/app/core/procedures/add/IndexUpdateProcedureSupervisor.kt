package org.archivekeep.app.core.procedures.add

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.BaseJobState
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.files.performance.RepoPerformanceTracker
import org.archivekeep.files.procedures.indexupdate.ImmediateIndexUpdateProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.procedures.ProcedureExecutionState

interface IndexUpdateProcedureSupervisor {
    val currentJobFlow: StateFlow<JobWrapper<JobState>?>

    fun discover(): Flow<Loadable<Discovery>>

    sealed interface State

    sealed interface JobState :
        State,
        BaseJobState

    data class PreparedAddJobState(
        val addProgress: IndexUpdateAddProgress,
        val moveProgress: IndexUpdateMoveProgress,
        override val progressLog: StateFlow<List<String>>,
        override val executionState: ProcedureExecutionState,
        override val performanceTracker: RepoPerformanceTracker,
    ) : State,
        JobState

    data class ImmediateAddJobState(
        val progress: ImmediateIndexUpdateProgress,
        override val progressLog: StateFlow<List<String>>,
        override val executionState: ProcedureExecutionState,
        override val performanceTracker: RepoPerformanceTracker,
    ) : State,
        JobState

    data class Discovery(
        val result: IndexUpdateProcedure.Discovery,
        val launch: (launchOptions: IndexUpdateProcedure.LaunchOptions) -> Unit,
        val startImmediate: () -> Unit,
    ) : State
}
