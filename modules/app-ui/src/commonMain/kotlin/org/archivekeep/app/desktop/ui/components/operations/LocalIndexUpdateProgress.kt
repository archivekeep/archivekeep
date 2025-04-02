package org.archivekeep.app.desktop.ui.components.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.desktop.ui.designsystem.dialog.LabelText
import org.archivekeep.app.desktop.ui.designsystem.progress.ProgressRow
import org.archivekeep.app.desktop.ui.designsystem.progress.ProgressRowList
import org.archivekeep.files.operations.indexupdate.AddOperation
import org.archivekeep.files.operations.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.operations.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.utils.filesAutoPlural

@Composable
fun LocalIndexUpdateProgress(
    selectedMoves: Set<AddOperation.PreparationResult.Move>,
    selectedFilesToAdd: Set<String>,
    moveProgress: IndexUpdateMoveProgress,
    addProgress: IndexUpdateAddProgress,
) {
    LabelText("Local index update")

    ProgressRowList {
        if (selectedMoves.isNotEmpty()) {
            ProgressRow(progress = {
                moveProgress.moved.size / selectedMoves.size.toFloat()
            }, "Moved ${moveProgress.moved.size} of ${selectedMoves.size} ${filesAutoPlural(selectedMoves)}")
        }
        if (selectedFilesToAdd.isNotEmpty()) {
            ProgressRow(
                progress = { addProgress.added.size / selectedFilesToAdd.size.toFloat() },
                "Added ${addProgress.added.size} of ${selectedFilesToAdd.size} ${filesAutoPlural(selectedFilesToAdd)}",
            )
        }
    }
}
