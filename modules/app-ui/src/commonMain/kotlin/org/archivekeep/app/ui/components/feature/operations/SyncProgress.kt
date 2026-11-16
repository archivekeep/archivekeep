package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.app.ui.utils.filesAutoPluralTranslated
import org.archivekeep.app.ui.utils.toUiString
import org.archivekeep.files.procedures.sync.discovery.DiscoveredAdditiveRelocationsGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredNewFilesGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredRelocationsMoveApplyGroup
import org.archivekeep.files.procedures.sync.job.SyncProcedureJobTask.ProgressSummary
import org.archivekeep.files.procedures.sync.operations.SyncOperation
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_unknown
import org.archivekeep.ui.resources.sync_copied
import org.archivekeep.ui.resources.sync_relocated
import org.archivekeep.ui.resources.sync_remaining_format
import org.archivekeep.ui.resources.sync_replicated
import org.archivekeep.utils.procedures.tasks.TaskExecutionProgressSummary
import org.jetbrains.compose.resources.stringResource

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
                        stringResource(Res.string.common_unknown),
                    )
            }
        }
    }
}

@Composable
fun <T : SyncOperation> SyncProcedureJobTaskProgress(progress: ProgressSummary<T>) {
    val group = progress.discoveryOperationGroup

    val text = when (group) {
        is DiscoveredAdditiveRelocationsGroup ->
            stringResource(Res.string.sync_replicated, progress.completedOperations.size, filesAutoPluralTranslated(progress.allOperations))

        is DiscoveredNewFilesGroup ->
            stringResource(Res.string.sync_copied, progress.completedOperations.size, filesAutoPluralTranslated(progress.allOperations))

        is DiscoveredRelocationsMoveApplyGroup ->
            stringResource(Res.string.sync_relocated, progress.completedOperations.size, filesAutoPluralTranslated(progress.allOperations))
    }

    val finalText = progress.timeEstimated?.let { timeEstimated ->
        stringResource(Res.string.sync_remaining_format, text, timeEstimated.toUiString())
    } ?: text

    ProgressRow(progress = { progress.completion }, finalText)
}
