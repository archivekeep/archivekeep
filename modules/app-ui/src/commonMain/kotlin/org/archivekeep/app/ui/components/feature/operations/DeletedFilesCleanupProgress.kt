package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.core.procedures.deletedcleanup.DeletedFilesCleanupProcedureSupervisor
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.utils.text.filesAutoPlural

@Composable
fun DeletedFilesCleanupProgress(jobState: DeletedFilesCleanupProcedureSupervisor.JobState) {
    LabelText("Deleted files cleanup")

    ProgressRowList {
        val selectedFilesToRemove = jobState.deletedFilesCleanupProgress.filesToRemove
        if (selectedFilesToRemove.isNotEmpty()) {
            ProgressRow(progress = {
                jobState.deletedFilesCleanupProgress.removedFiles.size / selectedFilesToRemove.size.toFloat()
            }, "Removed ${jobState.deletedFilesCleanupProgress.removedFiles.size} of ${filesAutoPlural(selectedFilesToRemove)}")
        }
    }
}
