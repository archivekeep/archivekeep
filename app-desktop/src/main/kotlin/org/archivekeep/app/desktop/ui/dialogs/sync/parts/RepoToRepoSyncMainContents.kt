package org.archivekeep.app.desktop.ui.dialogs.sync.parts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import org.archivekeep.app.core.operations.derived.SyncOperationExecution
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.dialogs.sync.RepoToRepoSyncUserFlow
import org.archivekeep.app.desktop.ui.dialogs.sync.describePreparedSyncOperationWithDetails

@Composable
fun (ColumnScope).RepoToRepoSyncMainContents(userFlowState: RepoToRepoSyncUserFlow.State) {
    LoadableGuard(userFlowState.operation) { operation ->
        when (operation) {
            is SyncOperationExecution.Prepared -> {
                val t =
                    remember(operation.preparedSyncOperation) {
                        describePreparedSyncOperationWithDetails(operation.preparedSyncOperation, "Upload")
                    }

                Text("Prepared")
                Box(
                    modifier =
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .weight(weight = 1f, fill = false),
                ) {
                    Text(
                        t,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        softWrap = true,
                    )
                }
            }
            is SyncOperationExecution.Running -> {
                val t by operation.progressLog.collectAsState("")

                Column {
                    Text("Running")
                    Box(
                        modifier =
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .weight(weight = 1f, fill = false),
                    ) {
                        Text(t)
                    }
                }
            }

            is SyncOperationExecution.Finished ->
                Column {
                    Text("Finished")
                    Box(
                        modifier =
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .weight(weight = 1f, fill = false),
                    ) {
                        Text(operation.progressLog, softWrap = true)
                    }
                }
        }
    }
}
