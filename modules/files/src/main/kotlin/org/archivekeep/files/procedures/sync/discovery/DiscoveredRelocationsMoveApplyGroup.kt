package org.archivekeep.files.procedures.sync.discovery

import org.archivekeep.files.api.repository.operations.CompareOperation
import org.archivekeep.files.procedures.sync.job.SyncProcedureJobTask
import org.archivekeep.files.procedures.sync.operations.RelocationApplyOperation
import org.archivekeep.utils.text.filesAutoPlural

class DiscoveredRelocationsMoveApplyGroup(
    toApply: List<RelocationApplyOperation>,
    val toIgnore: List<CompareOperation.Result.Relocation>,
) : DiscoveredSyncOperationsGroup<RelocationApplyOperation>(toApply) {
    override fun summaryText(progress: SyncProcedureJobTask.ProgressSummary<RelocationApplyOperation>): String =
        "relocated ${progress.completedOperations.size} of ${filesAutoPlural(progress.allOperations)}"
}
