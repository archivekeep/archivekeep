package org.archivekeep.app.desktop.ui.components.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.desktop.ui.designsystem.progress.ProgressRow
import org.archivekeep.app.desktop.ui.designsystem.progress.ProgressRowList
import org.archivekeep.files.operations.sync.SyncSubOperationGroup

@Composable
fun SyncProgress(progress: List<SyncSubOperationGroup.Progress>) {
    ProgressRowList {
        progress.forEach { group ->
            ProgressRow(
                progress = group::progress,
                group.summaryText(),
            )
        }
    }
}
