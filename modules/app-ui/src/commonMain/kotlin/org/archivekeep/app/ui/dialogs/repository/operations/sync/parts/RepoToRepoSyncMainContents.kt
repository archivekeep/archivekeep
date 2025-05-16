package org.archivekeep.app.ui.dialogs.repository.operations.sync.parts

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.operations.sync.RepoToRepoSync.JobState
import org.archivekeep.app.core.operations.sync.RepoToRepoSync.State
import org.archivekeep.app.core.utils.operations.OperationExecutionState
import org.archivekeep.app.ui.components.base.layout.IntrinsicSizeWrapperLayout
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumn
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.components.feature.dialogs.operations.ExecutionErrorIfPresent
import org.archivekeep.app.ui.components.feature.manyselect.ManySelectForRender
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectForRenderFromState
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectForRenderFromStateAnnotated
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectWithMergedState
import org.archivekeep.app.ui.components.feature.operations.InProgressOperationsList
import org.archivekeep.app.ui.components.feature.operations.ScrollableLogTextInDialog
import org.archivekeep.app.ui.components.feature.operations.SyncProgress
import org.archivekeep.app.ui.dialogs.repository.operations.sync.RepoToRepoSyncUserFlow
import org.archivekeep.app.ui.dialogs.repository.operations.sync.describe
import org.archivekeep.files.operations.sync.AdditiveRelocationsSyncStep
import org.archivekeep.files.operations.sync.NewFilesSyncStep
import org.archivekeep.files.operations.sync.RelocationsMoveApplySyncStep
import org.archivekeep.files.operations.sync.SyncSubOperationGroup

@Composable
fun (ColumnScope).RepoToRepoSyncMainContents(userFlowState: RepoToRepoSyncUserFlow.State) {
    LoadableGuard(userFlowState.operation) { operation ->
        when (operation) {
            is State.Prepared -> {
                val blocks =
                    operation.preparedSyncOperation.steps
                        .map<SyncSubOperationGroup<*>, ManySelectForRender<*, *, *>> { step ->
                            when (step) {
                                is AdditiveRelocationsSyncStep -> {
                                    val state = rememberManySelectWithMergedState(step.subOperations, userFlowState.selectedOperations)

                                    rememberManySelectForRenderFromState(
                                        state,
                                        "Existing files to replicate:",
                                        allItemsLabel = { "All $it replications" },
                                        itemLabelText = { it.describe() },
                                    )
                                }
                                is NewFilesSyncStep -> {
                                    val state = rememberManySelectWithMergedState(step.subOperations, userFlowState.selectedOperations)

                                    rememberManySelectForRenderFromState(
                                        state,
                                        "New files to copy:",
                                        allItemsLabel = { "All $it new files" },
                                        itemLabelText = { it.unmatchedBaseExtra.filenames.let { if (it.size == 1) it[0] else it.toString() } },
                                    )
                                }
                                is RelocationsMoveApplySyncStep -> {
                                    val state = rememberManySelectWithMergedState(step.subOperations, userFlowState.selectedOperations)

                                    rememberManySelectForRenderFromStateAnnotated(
                                        state = state,
                                        label = "Relocations to execute:",
                                        allItemsLabel = { "All relocations ($it)" },
                                        itemAnnotatedLabel = { it.describe() },
                                    )
                                }
                            }
                        }

                val guessedWidth = blocks.maxOf { it.guessedWidth }

                IntrinsicSizeWrapperLayout(
                    minIntrinsicWidth = guessedWidth,
                    maxIntrinsicWidth = guessedWidth,
                ) {
                    ScrollableLazyColumn(Modifier.weight(1f, fill = false)) {
                        blocks.forEach { it.render(this) }
                    }
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
                InProgressOperationsList(operation.inProgressOperationsStats.collectAsState().value)
                Spacer(Modifier.height(8.dp))
                ScrollableLogTextInDialog(operation.progressLog.collectAsState("").value)
                ExecutionErrorIfPresent(operation.executionState)
            }
        }
    }
}
