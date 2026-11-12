package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.dialog_index_update_found_moved
import org.archivekeep.ui.resources.dialog_index_update_found_new
import org.archivekeep.utils.text.filesAutoPlural
import org.jetbrains.compose.resources.stringResource

@Composable
fun IndexUpdatePreparationProgress(progress: IndexUpdateProcedure.PreparationProgress) {
    ProgressRowList {
        ProgressRow(
            progress = { progress.checkedFiles.size / progress.filesToCheck.size.toFloat() },
            "Checked ${progress.checkedFiles.size} of ${filesAutoPlural(progress.filesToCheck, "unindexed ")}",
        ) {
            if (progress.newFiles.isNotEmpty()) {
                Text(stringResource(Res.string.dialog_index_update_found_new, filesAutoPlural(progress.newFiles, "")))
            }
            if (progress.moves.isNotEmpty()) {
                Text(stringResource(Res.string.dialog_index_update_found_moved, filesAutoPlural(progress.moves, "")))
            }
        }
    }
}
