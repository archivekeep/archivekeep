package org.archivekeep.app.core.utils.operations

sealed interface OperationExecutionState {
    data object NotStarted : OperationExecutionState

    data object Running : OperationExecutionState

    data class Finished(
        val error: Throwable?,
    ) : OperationExecutionState
}
