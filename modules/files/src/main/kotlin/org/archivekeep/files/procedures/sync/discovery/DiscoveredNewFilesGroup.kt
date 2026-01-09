package org.archivekeep.files.procedures.sync.discovery

import org.archivekeep.files.procedures.sync.job.SyncProcedureJobTask
import org.archivekeep.files.procedures.sync.operations.CopyNewFileOperation
import org.archivekeep.utils.text.filesAutoPlural

class DiscoveredNewFilesGroup(
    unmatchedBaseExtras: List<CopyNewFileOperation>,
) : DiscoveredSyncOperationsGroup<CopyNewFileOperation>(unmatchedBaseExtras) {
    override fun summaryText(progress: SyncProcedureJobTask.ProgressSummary<CopyNewFileOperation>): String =
        "copied ${progress.completedOperations.size} of ${filesAutoPlural(progress.allOperations)}"
}
