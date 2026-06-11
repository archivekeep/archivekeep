package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.files.procedures.reindex.FileReindexProgress
import org.archivekeep.utils.text.filesAutoPlural

@Composable
fun FileReindexProgress(fileReindexProgress: FileReindexProgress) {
    LabelText("Local index update")

    ProgressRowList {
        val selectedFilesToReindex = fileReindexProgress.filesToReindex
        if (selectedFilesToReindex.isNotEmpty()) {
            ProgressRow(progress = {
                fileReindexProgress.reindexedFiles.size / selectedFilesToReindex.size.toFloat()
            }, "Reindexed ${fileReindexProgress.reindexedFiles.size} of ${filesAutoPlural(selectedFilesToReindex)}")
        }
    }
}
