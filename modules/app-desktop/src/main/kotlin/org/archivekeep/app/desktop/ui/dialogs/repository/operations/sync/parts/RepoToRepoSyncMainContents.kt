package org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync.parts

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.operations.sync.RepoToRepoSync.JobState
import org.archivekeep.app.core.operations.sync.RepoToRepoSync.State
import org.archivekeep.app.core.utils.operations.OperationExecutionState
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.components.dialogs.operations.ExecutionErrorIfPresent
import org.archivekeep.app.desktop.ui.components.itemManySelect
import org.archivekeep.app.desktop.ui.components.operations.ScrollableLogTextInDialog
import org.archivekeep.app.desktop.ui.components.operations.SyncProgress
import org.archivekeep.app.desktop.ui.components.rememberManySelectWithMergedState
import org.archivekeep.app.desktop.ui.designsystem.dialog.LabelText
import org.archivekeep.app.desktop.ui.designsystem.layout.scrollable.ScrollableLazyColumn
import org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync.RepoToRepoSyncUserFlow
import org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync.describe
import org.archivekeep.files.operations.sync.AdditiveRelocationsSyncStep
import org.archivekeep.files.operations.sync.NewFilesSyncStep
import org.archivekeep.files.operations.sync.RelocationsMoveApplySyncStep
import org.archivekeep.files.operations.sync.SyncSubOperationGroup

@Composable
fun (ColumnScope).RepoToRepoSyncMainContents(userFlowState: RepoToRepoSyncUserFlow.State) {
    LoadableGuard(userFlowState.operation) { operation ->
        when (operation) {
            is State.Prepared -> {
                val renderables =
                    operation.preparedSyncOperation.steps
                        .map<SyncSubOperationGroup<*>, LazyListScope.() -> Unit> { step ->
                            when (step) {
                                is AdditiveRelocationsSyncStep -> {
                                    val state = rememberManySelectWithMergedState(step.subOperations, userFlowState.selectedOperations)

                                    return@map {
                                        itemManySelect(
                                            "Existing files to replicate:",
                                            allItemsLabel = { "All $it replications" },
                                            itemLabelText = { it.describe() },
                                            state = state,
                                        )
                                    }
                                }
                                is NewFilesSyncStep -> {
                                    val state = rememberManySelectWithMergedState(step.subOperations, userFlowState.selectedOperations)

                                    return@map {
                                        itemManySelect(
                                            "New files to copy:",
                                            allItemsLabel = { "All $it new files" },
                                            itemLabelText = { it.unmatchedBaseExtra.filenames.let { if (it.size == 1) it[0] else it.toString() } },
                                            state = state,
                                        )
                                    }
                                }
                                is RelocationsMoveApplySyncStep -> {
                                    val state = rememberManySelectWithMergedState(step.subOperations, userFlowState.selectedOperations)

                                    return@map {
                                        itemManySelect(
                                            "Relocations to execute:",
                                            allItemsLabel = { "All relocations ($it)" },
                                            itemLabel = { Text(it.describe()) },
                                            state = state,
                                        )
                                    }
                                }
                            }
                        }

                ScrollableLazyColumn(Modifier.weight(1f, fill = false)) {
                    renderables.forEach { it() }
                }
            }
            is JobState -> {
                Spacer(Modifier.height(8.dp))

                LabelText(
                    when (val executionState = operation.executionState) {
                        OperationExecutionState.NotStarted -> "Starting"
                        OperationExecutionState.Running -> "Progress"
                        is OperationExecutionState.Finished ->
                            if (executionState.success) {
                                "Finished"
                            } else if (executionState.cancelled) {
                                "Cancelled"
                            } else {
                                "Failed"
                            }
                    },
                )

                SyncProgress(operation.progress.collectAsState().value)
                Spacer(Modifier.height(8.dp))
                ScrollableLogTextInDialog(operation.progressLog.collectAsState("").value)
                ExecutionErrorIfPresent(operation.executionState)
            }
        }
    }
}
