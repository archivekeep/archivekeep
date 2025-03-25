package org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync.parts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.operations.sync.RepoToRepoSync.JobState
import org.archivekeep.app.core.operations.sync.RepoToRepoSync.State
import org.archivekeep.app.desktop.ui.components.ItemManySelect
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.components.rememberManySelectWithMergedState
import org.archivekeep.app.desktop.ui.designsystem.layout.scrollable.ScrollableColumn
import org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync.RepoToRepoSyncUserFlow
import org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync.describe
import org.archivekeep.files.operations.sync.AdditiveRelocationsSyncStep
import org.archivekeep.files.operations.sync.NewFilesSyncStep
import org.archivekeep.files.operations.sync.RelocationsMoveApplySyncStep

@Composable
fun (ColumnScope).RepoToRepoSyncMainContents(userFlowState: RepoToRepoSyncUserFlow.State) {
    LoadableGuard(userFlowState.operation) { operation ->
        when (operation) {
            is State.Prepared -> {
                ScrollableColumn(Modifier.weight(1f, fill = false)) {
                    operation.preparedSyncOperation.steps.forEach { step ->
                        Spacer(Modifier.height(12.dp))
                        when (step) {
                            is AdditiveRelocationsSyncStep -> {
                                ItemManySelect(
                                    "Existing files to replicate:",
                                    allItemsLabel = { "All $it replications" },
                                    itemLabelText = { it.describe() },
                                    state = rememberManySelectWithMergedState(step.subOperations, userFlowState.selectedOperations),
                                )
                            }
                            is NewFilesSyncStep -> {
                                ItemManySelect(
                                    "New files to copy:",
                                    allItemsLabel = { "All $it new files" },
                                    itemLabelText = { it.unmatchedBaseExtra.filenames.let { if (it.size == 1) it[0] else it.toString() } },
                                    state = rememberManySelectWithMergedState(step.subOperations, userFlowState.selectedOperations),
                                )
                            }
                            is RelocationsMoveApplySyncStep -> {
                                ItemManySelect(
                                    "Relocations to execute:",
                                    allItemsLabel = { "All relocations ($it)" },
                                    itemLabel = { Text(it.describe()) },
                                    state = rememberManySelectWithMergedState(step.subOperations, userFlowState.selectedOperations),
                                )
                            }
                        }
                    }
                }
            }
            is JobState.Created -> {
                Text("Starting")
            }
            is JobState.Running -> {
                val t by operation.progressLog.collectAsState("")

                Column {
                    Text("Running")
                    ScrollableColumn(modifier = Modifier.weight(weight = 1f, fill = false)) {
                        Text(t)
                    }
                }
            }

            is JobState.Finished ->
                Column {
                    Text("Finished")
                    ScrollableColumn(modifier = Modifier.weight(weight = 1f, fill = false)) {
                        Text(operation.progressLog, softWrap = true)
                    }
                }
        }
    }
}
