package org.archivekeep.app.core.utils.generics

sealed interface ExecutionOutcome {
    open class Success : ExecutionOutcome

    open class Failed(
        val cause: Throwable = RuntimeException("Unknown cause"),
    ) : ExecutionOutcome
}
