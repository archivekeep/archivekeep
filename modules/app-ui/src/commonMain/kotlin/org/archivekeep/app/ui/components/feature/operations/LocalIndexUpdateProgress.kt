package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.core.procedures.add.IndexUpdateProcedureSupervisor
import org.archivekeep.app.core.procedures.add.IndexUpdateProcedureSupervisor.PreparedAddJobState
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.files.procedures.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.procedures.indexupdate.ImmediateIndexUpdateProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.dialog_index_update_title
import org.archivekeep.utils.text.filesAutoPlural
import org.jetbrains.compose.resources.stringResource

@Composable
fun LocalIndexUpdateProgress(state: PreparedAddJobState) {
    LocalIndexUpdateProgress(state.moveProgress, state.addProgress)
}

@Composable
fun LocalIndexUpdateProgress(state: IndexUpdateProcedureSupervisor.ImmediateAddJobState) {
    LocalIndexUpdateProgress(state.progress)
}

@Composable
fun LocalIndexUpdateProgress(
    moveProgress: IndexUpdateMoveProgress,
    addProgress: IndexUpdateAddProgress,
) {
    LabelText(stringResource(Res.string.dialog_index_update_title))

    ProgressRowList {
        val selectedMoves = moveProgress.movesToExecute
        if (selectedMoves.isNotEmpty()) {
            ProgressRow(progress = {
                moveProgress.moved.size / selectedMoves.size.toFloat()
            }, "Moved ${moveProgress.moved.size} of ${filesAutoPlural(selectedMoves)}")
        }
        val selectedFilesToAdd = addProgress.filesToAdd
        if (selectedFilesToAdd.isNotEmpty()) {
            ProgressRow(
                progress = { addProgress.added.size / selectedFilesToAdd.size.toFloat() },
                "Added ${addProgress.added.size} of ${filesAutoPlural(selectedFilesToAdd)}",
            )
        }
    }
}

@Composable
fun LocalIndexUpdateProgress(progress: ImmediateIndexUpdateProgress) {
    LabelText(stringResource(Res.string.dialog_index_update_title))

    ProgressRowList {
        val filesToAdd = progress.filesToAdd
        if (filesToAdd.isNotEmpty()) {
            ProgressRow(progress = {
                progress.addedNew.size / filesToAdd.size.toFloat()
            }, "Added ${progress.addedNew.size} of ${filesAutoPlural(filesToAdd)}")
        }
    }
}
