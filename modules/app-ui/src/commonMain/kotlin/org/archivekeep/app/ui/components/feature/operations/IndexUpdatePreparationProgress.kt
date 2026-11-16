package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.app.ui.utils.filesAutoPluralTranslated
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.dialog_index_update_checked
import org.archivekeep.ui.resources.dialog_index_update_found_moved
import org.archivekeep.ui.resources.dialog_index_update_found_new
import org.archivekeep.ui.resources.index_unindexed
import org.jetbrains.compose.resources.stringResource

@Composable
fun IndexUpdatePreparationProgress(progress: IndexUpdateProcedure.DiscoveryProgress) {
    ProgressRowList {
        ProgressRow(
            progress = { progress.checkedFiles.size / progress.filesToCheck.size.toFloat() },
            stringResource(Res.string.dialog_index_update_checked, progress.checkedFiles.size, filesAutoPluralTranslated(progress.filesToCheck, stringResource(Res.string.index_unindexed))),
        ) {
            if (progress.newFiles.isNotEmpty()) {
                Text(stringResource(Res.string.dialog_index_update_found_new, filesAutoPluralTranslated(progress.newFiles, "")))
            }
            if (progress.moves.isNotEmpty()) {
                Text(stringResource(Res.string.dialog_index_update_found_moved, filesAutoPluralTranslated(progress.moves, "")))
            }
        }
    }
}
