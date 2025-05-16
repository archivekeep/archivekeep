package org.archivekeep.utils.procedures

interface ProcedureExecutionContext {
    suspend fun runOperation(block: suspend (context: OperationContext) -> Unit)
}
