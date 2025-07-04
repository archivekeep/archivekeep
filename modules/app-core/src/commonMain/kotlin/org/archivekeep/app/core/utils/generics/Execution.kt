package org.archivekeep.app.core.utils.generics

import kotlinx.coroutines.flow.MutableStateFlow

sealed interface Execution {
    data object NotRunning : Execution

    data object InProgress : Execution

    data class Finished(
        val outcome: ExecutionOutcome,
    ) : Execution
}

inline fun MutableStateFlow<in Execution>.perform(block: () -> Unit) {
    try {
        value = Execution.InProgress

        block()

        value = Execution.Finished(ExecutionOutcome.Success())
    } catch (e: Throwable) {
        value = Execution.Finished(ExecutionOutcome.Failed(e))
        throw e
    }
}
