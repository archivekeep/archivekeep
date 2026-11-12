package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.core.procedures.reindex.FileReindexProcedureSupervisor
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.dialog_reindex_title
import org.archivekeep.utils.text.filesAutoPlural
import org.jetbrains.compose.resources.stringResource

@Composable
fun FileReindexProgress(jobState: FileReindexProcedureSupervisor.JobState) {
    LabelText(stringResource(Res.string.dialog_reindex_title))

    ProgressRowList {
        val selectedFilesToReindex = jobState.reindexProgress.filesToReindex
        if (selectedFilesToReindex.isNotEmpty()) {
            ProgressRow(progress = {
                jobState.reindexProgress.reindexedFiles.size / selectedFilesToReindex.size.toFloat()
            }, "Reindexed ${jobState.reindexProgress.reindexedFiles.size} of ${filesAutoPlural(selectedFilesToReindex)}")
        }
    }
}
