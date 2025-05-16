package org.archivekeep.app.core.procedures.utils

import kotlinx.coroutines.CancellationException

sealed interface ProcedureExecutionState {
    data object NotStarted : ProcedureExecutionState

    data object Running : ProcedureExecutionState

    data class Finished(
        val error: Throwable?,
    ) : ProcedureExecutionState {
        val success = error == null
        val cancelled = error is CancellationException
    }
}
