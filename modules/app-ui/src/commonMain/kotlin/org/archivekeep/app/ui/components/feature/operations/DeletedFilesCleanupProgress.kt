package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.files.procedures.deletedcleanup.DeletedFilesCleanupProgress
import org.archivekeep.utils.text.filesAutoPlural

@Composable
fun DeletedFilesCleanupProgress(fileReindexProgress: DeletedFilesCleanupProgress) {
    LabelText("Deleted files cleanup")

    ProgressRowList {
        val selectedFilesToReindex = fileReindexProgress.filesToRemove
        if (selectedFilesToReindex.isNotEmpty()) {
            ProgressRow(progress = {
                fileReindexProgress.removedFiles.size / selectedFilesToReindex.size.toFloat()
            }, "Removed ${fileReindexProgress.removedFiles.size} of ${filesAutoPlural(selectedFilesToReindex)}")
        }
    }
}
