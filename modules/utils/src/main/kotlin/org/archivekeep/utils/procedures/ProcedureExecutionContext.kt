package org.archivekeep.utils.procedures

import org.archivekeep.utils.procedures.operations.OperationContext

interface ProcedureExecutionContext {
    suspend fun runOperation(block: suspend (context: OperationContext) -> Unit)
}
