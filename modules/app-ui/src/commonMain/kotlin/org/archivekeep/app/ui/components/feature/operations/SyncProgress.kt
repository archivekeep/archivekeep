package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.app.ui.utils.toUiString
import org.archivekeep.files.procedures.sync.SyncProcedureJobTask.ProgressSummary
import org.archivekeep.files.procedures.sync.operations.SyncOperation
import org.archivekeep.utils.procedures.tasks.TaskExecutionProgressSummary

@Composable
fun SyncProgress(subProgressList: List<TaskExecutionProgressSummary>) {
    ProgressRowList {
        subProgressList.forEach { subProgress ->
            when (subProgress) {
                is ProgressSummary<*> ->
                    SyncProcedureJobTaskProgress(subProgress)

                is TaskExecutionProgressSummary.Group ->
                    SyncProgress(subProgress.subTasks)

                is TaskExecutionProgressSummary.Simple ->
                    ProgressRow(
                        progress = { subProgress.completion },
                        "Unknown",
                    )
            }
        }
    }
}

@Composable
fun <T : SyncOperation> SyncProcedureJobTaskProgress(progress: ProgressSummary<T>) {
    val group = progress.discoveryOperationGroup

    var text = group.summaryText(progress)

    progress.timeEstimated?.let { timeEstimated ->
        text = "$text [remaining ${timeEstimated.toUiString()}]"
    }

    ProgressRow(progress = { progress.completion }, text)
}
