package org.archivekeep.app.ui.dialogs.repository.procedures.sync.parts

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync.JobState
import org.archivekeep.app.core.procedures.sync.RepoToRepoSync.State
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
import org.archivekeep.app.ui.dialogs.repository.procedures.sync.RepoToRepoSyncUserFlow
import org.archivekeep.app.ui.dialogs.repository.procedures.sync.describe
import org.archivekeep.files.procedures.sync.discovery.DiscoveredAdditiveRelocationsGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredNewFilesGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredRelocationsMoveApplyGroup
import org.archivekeep.files.procedures.sync.discovery.DiscoveredSyncOperationsGroup
import org.archivekeep.utils.procedures.ProcedureExecutionState

@Composable
fun (ColumnScope).RepoToRepoSyncMainContents(userFlowState: RepoToRepoSyncUserFlow.State) {
    LoadableGuard(userFlowState.operation) { operation ->
        when (operation) {
            is State.Prepared -> {
                val blocks =
                    operation.discoveredSync.groups
                        .map<DiscoveredSyncOperationsGroup<*>, ManySelectForRender<*, *, *>> { step ->
                            when (step) {
                                is DiscoveredAdditiveRelocationsGroup -> {
                                    val state = rememberManySelectWithMergedState(step.operations, userFlowState.selectedOperations)

                                    rememberManySelectForRenderFromState(
                                        state,
                                        "Existing files to replicate:",
                                        allItemsLabel = { "All $it replications" },
                                        itemLabelText = { it.describe() },
                                    )
                                }
                                is DiscoveredNewFilesGroup -> {
                                    val state = rememberManySelectWithMergedState(step.operations, userFlowState.selectedOperations)

                                    rememberManySelectForRenderFromState(
                                        state,
                                        "New files to copy:",
                                        allItemsLabel = { "All $it new files" },
                                        itemLabelText = { it.unmatchedBaseExtra.filenames.let { if (it.size == 1) it[0] else it.toString() } },
                                    )
                                }
                                is DiscoveredRelocationsMoveApplyGroup -> {
                                    val state = rememberManySelectWithMergedState(step.operations, userFlowState.selectedOperations)

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
                Spacer(Modifier.height(8.dp))
                ScrollableLogTextInDialog(operation.progressLog.collectAsState("").value)
                ExecutionErrorIfPresent(operation.executionState)
            }
        }
    }
}
