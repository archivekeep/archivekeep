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
import org.archivekeep.utils.procedures.ProcedureExecutionState

@Composable
internal fun SyncJobState(operation: RepoToRepoSync.JobState) {
    Spacer(Modifier.height(12.dp))

    OperationProgressTabs(
        summaryContent = {
            LabelText(
                when (val executionState = operation.executionState) {
                    ProcedureExecutionState.NotStarted -> {
                        "Starting"
                    }

                    ProcedureExecutionState.Running -> {
                        "Progress"
                    }

                    is ProcedureExecutionState.Finished -> {
                        if (executionState.success) {
                            "Finished"
                        } else if (executionState.cancelled) {
                            "Cancelled"
                        } else {
                            "Failed"
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
