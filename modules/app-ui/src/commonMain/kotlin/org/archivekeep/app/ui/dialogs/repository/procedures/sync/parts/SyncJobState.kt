package org.archivekeep.app.ui.dialogs.repository.procedures.sync.parts

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.feature.dialogs.operations.ExecutionErrorIfPresent
import org.archivekeep.app.ui.components.feature.operations.InProgressOperationsList
import org.archivekeep.app.ui.components.feature.operations.ScrollableLogTextInDialog
import org.archivekeep.app.ui.components.feature.operations.SyncProgress
import org.archivekeep.utils.procedures.ProcedureExecutionState

private const val TAB_SUMMARY = "summary";
private const val TAB_LOG = "log";
private const val TAB_ERROR_LOG = "error-log";

private val tabOptions = listOf(
    TAB_SUMMARY to "Summary",
    TAB_LOG to "Log",
    TAB_ERROR_LOG to "Errors"
)

@Composable
internal fun SyncJobState(operation: RepoToRepoSync.JobState) {
    var selectedTab by remember { mutableStateOf(TAB_SUMMARY) }

    val errorLog = operation.errorLog.collectAsState("").value

    Spacer(Modifier.height(12.dp))

    SingleChoiceSegmentedButtonRow {
        tabOptions.forEachIndexed { index, tab ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = tabOptions.size
                ),
                onClick = { selectedTab = tab.first },
                selected = selectedTab == tab.first,
                label = {
                    Row {
                        Text(tab.second)

                        if (tab.first == TAB_ERROR_LOG && errorLog.isNotBlank()) {
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                color = Color.Red,
                                text = errorLog.trim().lines().size.toString(),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    when (selectedTab) {
        TAB_SUMMARY -> {
            LabelText(
                when (val executionState = operation.executionState) {
                    ProcedureExecutionState.NotStarted -> "Starting"
                    ProcedureExecutionState.Running -> "Progress"
                    is ProcedureExecutionState.Finished ->
                        if (executionState.success) {
                            "Finished"
                        } else if (executionState.cancelled) {
                            "Cancelled"
                        } else {
                            "Failed"
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
        }

        TAB_LOG -> ScrollableLogTextInDialog(operation.progressLog.collectAsState("").value)
        TAB_ERROR_LOG -> ScrollableLogTextInDialog(errorLog)
    }

    ExecutionErrorIfPresent(operation.executionState)
}
