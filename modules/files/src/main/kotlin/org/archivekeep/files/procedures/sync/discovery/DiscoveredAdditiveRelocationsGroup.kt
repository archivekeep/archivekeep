package org.archivekeep.files.procedures.sync.discovery

import org.archivekeep.files.procedures.sync.job.SyncProcedureJobTask
import org.archivekeep.files.procedures.sync.operations.AdditiveReplicationOperation
import org.archivekeep.utils.text.filesAutoPlural

class DiscoveredAdditiveRelocationsGroup(
    steps: List<AdditiveReplicationOperation>,
) : DiscoveredSyncOperationsGroup<AdditiveReplicationOperation>(steps) {
    override fun summaryText(progress: SyncProcedureJobTask.ProgressSummary<AdditiveReplicationOperation>): String =
        "replicated ${progress.completedOperations.size} of ${filesAutoPlural(progress.allOperations)}"
}
