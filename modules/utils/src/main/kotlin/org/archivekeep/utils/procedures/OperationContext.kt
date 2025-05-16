package org.archivekeep.utils.procedures

interface OperationContext {
    fun progressReport(progress: OperationProgress)
}
