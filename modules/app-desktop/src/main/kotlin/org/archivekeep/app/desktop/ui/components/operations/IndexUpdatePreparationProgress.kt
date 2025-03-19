package org.archivekeep.app.desktop.ui.components.operations

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.archivekeep.app.desktop.ui.designsystem.progress.ProgressRow
import org.archivekeep.app.desktop.ui.designsystem.progress.ProgressRowList
import org.archivekeep.app.desktop.ui.utils.filesAutoPlural
import org.archivekeep.files.operations.AddOperation

@Composable
fun IndexUpdatePreparationProgress(progress: AddOperation.PreparationProgress) {
    ProgressRowList {
        ProgressRow(
            progress = { progress.checkedFiles.size / progress.filesToCheck.size.toFloat() },
            "Checked ${progress.checkedFiles.size} of ${progress.filesToCheck.size} unindexed ${filesAutoPlural(progress.filesToCheck)}",
        ) {
            if (progress.newFiles.isNotEmpty()) {
                Text("Found ${progress.newFiles.size} new ${filesAutoPlural(progress.newFiles)}")
            }
            if (progress.moves.isNotEmpty()) {
                Text("Found ${progress.moves} moved ${filesAutoPlural(progress.newFiles)}")
            }
        }
    }
}
