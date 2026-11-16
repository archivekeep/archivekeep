package org.archivekeep.app.core.procedures.verify

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.procedures.utils.BaseJobState
import org.archivekeep.app.core.procedures.utils.JobWrapper
import org.archivekeep.files.procedures.verify.VerifyContentsProcedure
import org.archivekeep.files.procedures.verify.VerifyProgress
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.procedures.ProcedureExecutionState

interface VerifySupervisor {
    val currentJobFlow: StateFlow<JobWrapper<JobState>?>

    fun prepare(): Flow<Loadable<Preparation>>

    sealed interface State

    data class JobState(
        val verifyProgress: VerifyProgress,
        override val progressLog: StateFlow<List<String>>,
        override val executionState: ProcedureExecutionState,
    ) : State,
        BaseJobState

    data class Preparation(
        val result: VerifyContentsProcedure.PreparationResult,
        val launch: () -> Unit,
    ) : State
}
