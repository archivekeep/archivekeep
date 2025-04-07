package org.archivekeep.app.core.utils.operations

import kotlinx.coroutines.CancellationException

sealed interface OperationExecutionState {
    data object NotStarted : OperationExecutionState

    data object Running : OperationExecutionState

    data class Finished(
        val error: Throwable?,
    ) : OperationExecutionState {
        val success = error == null
        val cancelled = error is CancellationException
    }
}
