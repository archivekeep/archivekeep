package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.utils.filesAutoPlural

@Composable
fun LocalIndexUpdateProgress(
    moveProgress: IndexUpdateMoveProgress,
    addProgress: IndexUpdateAddProgress,
) {
    LabelText("Local index update")

    ProgressRowList {
        val selectedMoves = moveProgress.movesToExecute
        if (selectedMoves.isNotEmpty()) {
            ProgressRow(progress = {
                moveProgress.moved.size / selectedMoves.size.toFloat()
            }, "Moved ${moveProgress.moved.size} of ${selectedMoves.size} ${filesAutoPlural(selectedMoves)}")
        }
        val selectedFilesToAdd = addProgress.filesToAdd
        if (selectedFilesToAdd.isNotEmpty()) {
            ProgressRow(
                progress = { addProgress.added.size / selectedFilesToAdd.size.toFloat() },
                "Added ${addProgress.added.size} of ${selectedFilesToAdd.size} ${filesAutoPlural(selectedFilesToAdd)}",
            )
        }
    }
}
