package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.utils.filesAutoPlural

@Composable
fun IndexUpdatePreparationProgress(progress: IndexUpdateProcedure.PreparationProgress) {
    ProgressRowList {
        ProgressRow(
            progress = { progress.checkedFiles.size / progress.filesToCheck.size.toFloat() },
            "Checked ${progress.checkedFiles.size} of ${filesAutoPlural(progress.filesToCheck, "unindexed ")}",
        ) {
            if (progress.newFiles.isNotEmpty()) {
                Text("Found ${filesAutoPlural(progress.newFiles, "new ")}")
            }
            if (progress.moves.isNotEmpty()) {
                Text("Found ${filesAutoPlural(progress.moves, "moved ")}")
            }
        }
    }
}
