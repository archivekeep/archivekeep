package org.archivekeep.app.desktop.ui.dialogs.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ButtonDefaults.buttonElevation
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.operations.derived.PreparedRunningOrCompletedSync
import org.archivekeep.app.core.operations.derived.SyncOperationExecution
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.generics.mapToLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.data.getSyncCandidatesAsStateFlow
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.domain.wiring.LocalSyncService
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayWithLoadableGuard
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.utils.collectAsLoadable
import org.archivekeep.app.desktop.utils.collectLoadableFlow
import org.archivekeep.app.desktop.utils.stickToFirstNotNullAsState
import org.archivekeep.files.operations.AdditiveRelocationsSyncStep
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.operations.NewFilesSyncStep
import org.archivekeep.files.operations.PreparedSyncOperation
import org.archivekeep.files.operations.RelocationSyncMode
import org.archivekeep.files.operations.RelocationsMoveApplySyncStep

class SyncOperationDialog(
    val repositoryURI: RepositoryURI,
) : Dialog {
    @OptIn(ExperimentalLayoutApi::class, DelicateCoroutinesApi::class)
    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val storageService = LocalStorageService.current
        val syncService = LocalSyncService.current

        val repoFlow =
            remember(repositoryURI) {
                storageService.repository(repositoryURI)
            }.collectAsLoadable()

        val (relocationSyncMode, setRelocationSyncMode) =
            remember {
                mutableStateOf<RelocationSyncMode>(
                    RelocationSyncMode.Move(
                        false,
                        false,
                    ),
                )
            }

        val otherRepositoryCandidates = getSyncCandidatesAsStateFlow(repositoryURI)

        var selectedTarget by remember { mutableStateOf<RepositoryURI?>(null) }

        val operationProgress =
            remember(repositoryURI, selectedTarget, relocationSyncMode) {
                selectedTarget?.let { targetId ->
                    val repoToRepoSync = syncService.getRepoToRepoSync(repositoryURI, targetId)

                    val rememberedOP = repoToRepoSync.currentlyRunningOperationFlow.stickToFirstNotNullAsState(GlobalScope)

                    rememberedOP.flatMapLatest {
                        if (it != null) {
                            it.currentState.map { it as PreparedRunningOrCompletedSync }.mapToLoadable()
                        } else {
                            repoToRepoSync
                                .prepare(relocationSyncMode)
                                .mapLoadedData { it as PreparedRunningOrCompletedSync }
                        }
                    }
                }
            }?.collectLoadableFlow()

        DialogOverlayWithLoadableGuard(repoFlow, onDismissRequest = onClose) { repo ->
            val title =
                remember(repo) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(repo.displayName)
                            append(": ")
                        }
                        append("upload")
                    }
                }

            DialogInnerContainer(title, content = {
                Text(
                    "To:",
                    style = MaterialTheme.typography.titleSmall,
                )
                LoadableGuard(otherRepositoryCandidates.value) { repos ->
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        repos.forEach { repo ->
                            OutlinedButton(
                                onClick = { selectedTarget = repo.uri },
                                shape = RoundedCornerShape(50),
                                elevation =
                                    buttonElevation(
                                        defaultElevation = 0.dp,
                                    ),
                                colors =
                                    if (selectedTarget == repo.uri) {
                                        buttonColors()
                                    } else {
                                        outlinedButtonColors()
                                    },
                            ) {
                                Text(repo.storage.displayName)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(0.dp, 8.dp))
                if (operationProgress == null) {
                    Text("Select target first")
                } else {
                    LoadableGuard(operationProgress) { operationProgress ->
                        ComparisonSummary(operationProgress.comparisonResult.value)

                        HorizontalDivider(modifier = Modifier.padding(0.dp, 8.dp))

                        when (operationProgress) {
                            is SyncOperationExecution.Prepared -> {
                                val t =
                                    remember(operationProgress.preparedSyncOperation) {
                                        describePreparedSyncOperation(operationProgress.preparedSyncOperation)
                                    }
                                Column {
                                    Text("Prepared")
                                    Box(
                                        modifier =
                                            Modifier
                                                .verticalScroll(rememberScrollState())
                                                .weight(weight = 1f, fill = false),
                                    ) {
                                        Text(t)
                                    }
                                    Button(onClick = {
                                        operationProgress.startExecution()
                                    }) {
                                        Text("Start execution")
                                    }
                                }
                            }

                            is SyncOperationExecution.Running -> {
                                val t by operationProgress.progressLog.collectAsState("")

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

                            is SyncOperationExecution.Finished -> {
                                Column {
                                    Text("Finished")
                                    Box(
                                        modifier =
                                            Modifier
                                                .verticalScroll(rememberScrollState())
                                                .weight(weight = 1f, fill = false),
                                    ) {
                                        Text(operationProgress.progressLog)
                                    }
                                }
                            }
                        }
                    }
                }
            }, bottomContent = {})
        }
    }
}

fun describePreparedSyncOperation(a: PreparedSyncOperation) =
    if (a.isNoOp()) {
        "NoOP"
    } else {
        a.steps.joinToString("\n") {
            when (it) {
                is AdditiveRelocationsSyncStep -> "Duplicate ${it.relocations.size} files."

                is RelocationsMoveApplySyncStep -> "Move ${it.relocations.size} files."

                is NewFilesSyncStep -> "Upload ${it.unmatchedBaseExtras.size} files."
            }
        }
    }

@Composable
private fun ComparisonSummary(cr: CompareOperation.Result) {
    val summaryText =
        remember(cr) {
            listOf(
                "Extra local files: ${cr.unmatchedBaseExtras.size}",
                "Extra files in destination: ${cr.unmatchedOtherExtras.size}",
                "Relocations: ${cr.relocations.size}",
            ).joinToString("\n")
        }

    Text(
        "Differences summary:",
        style = MaterialTheme.typography.titleSmall,
    )
    Text(summaryText, style = MaterialTheme.typography.bodySmall)
}
