package org.archivekeep.app.ui.dialogs.repository.procedures.sync.parts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.feature.operations.InProgressOperationsList
import org.archivekeep.app.ui.components.feature.operations.OperationProgressTabs
import org.archivekeep.app.ui.components.feature.operations.SyncProgress
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_cancelled
import org.archivekeep.ui.resources.common_failed
import org.archivekeep.ui.resources.common_finished
import org.archivekeep.ui.resources.common_progress
import org.archivekeep.ui.resources.common_starting
import org.archivekeep.utils.procedures.ProcedureExecutionState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SyncJobState(operation: RepoToRepoSync.JobState) {
    Spacer(Modifier.height(12.dp))

    OperationProgressTabs(
        summaryContent = {
            LabelText(
                when (val executionState = operation.executionState) {
                    ProcedureExecutionState.NotStarted -> {
                        stringResource(Res.string.common_starting)
                    }

                    ProcedureExecutionState.Running -> {
                        stringResource(Res.string.common_progress)
                    }

                    is ProcedureExecutionState.Finished -> {
                        if (executionState.success) {
                            stringResource(Res.string.common_finished)
                        } else if (executionState.cancelled) {
                            stringResource(Res.string.common_cancelled)
                        } else {
                            stringResource(Res.string.common_failed)
                        }
                    }
                },
            )
            SyncProgress(
                operation.progress
                    .collectAsState()
                    .value.subTasks,
            )
            Spacer(Modifier.height(8.dp))
            InProgressOperationsList(operation.inProgressOperationsProgress.collectAsState().value)
        },
        state = operation,
        errorLog = operation.errorLog.collectAsState(),
    )
}
