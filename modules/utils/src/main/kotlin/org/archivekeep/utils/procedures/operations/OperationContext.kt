package org.archivekeep.utils.procedures.operations

interface OperationContext {
    fun progressReport(progress: OperationProgress)
}
