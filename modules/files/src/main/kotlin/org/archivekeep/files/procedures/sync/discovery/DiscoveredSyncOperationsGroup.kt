package org.archivekeep.files.procedures.sync.discovery

import org.archivekeep.files.procedures.sync.job.SyncProcedureJobTask
import org.archivekeep.files.procedures.sync.operations.SyncOperation

sealed class DiscoveredSyncOperationsGroup<T : SyncOperation>(
    val operations: List<T>,
) {
    fun createJobTask(limitToSubset: Set<SyncOperation>?): SyncProcedureJobTask<T> {
        return SyncProcedureJobTask(
            this,
            operations
                .let { list ->
                    if (limitToSubset == null) {
                        list
                    } else {
                        list.filter { it in limitToSubset }
                    }
                },
        )
    }

    fun isNoOp(): Boolean = operations.isEmpty()

    abstract fun summaryText(progress: SyncProcedureJobTask.ProgressSummary<T>): String
}
