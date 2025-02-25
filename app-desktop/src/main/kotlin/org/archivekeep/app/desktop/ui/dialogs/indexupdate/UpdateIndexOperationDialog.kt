package org.archivekeep.app.desktop.ui.dialogs.indexupdate

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import org.archivekeep.app.core.operations.add.AddOperationSupervisor
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalAddOperationSupervisorService
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.ui.components.FileManySelect
import org.archivekeep.app.desktop.ui.components.ItemManySelect
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogCardWithDialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlay
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.utils.collectAsLoadable
import org.archivekeep.app.desktop.utils.collectLoadableFlow
import org.archivekeep.app.desktop.utils.derivedMutableStateOf
import org.archivekeep.app.desktop.utils.stickToFirstNotNull
import org.archivekeep.files.operations.AddOperation
import org.archivekeep.files.operations.AddOperation.PreparationResult.Move
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable

class UpdateIndexOperationDialog(
    val repositoryURI: RepositoryURI,
) : Dialog {
    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val repository =
            with(LocalRepoService.current) {
                remember(repositoryURI) {
                    this.getRepository(repositoryURI).informationFlow
                }
            }.collectAsLoadable()

        val operation =
            LocalAddOperationSupervisorService.current.let { service ->
                remember(service, repositoryURI) {
                    service.getAddOperation(repositoryURI)
                }
            }

        val launchOptions = remember { mutableStateOf(AddOperation.LaunchOptions(addFilesSubsetLimit = emptySet(), movesSubsetLimit = emptySet())) }

        val statusFlow: Loadable<AddOperationSupervisor.State> =
            remember(operation) {
                val f: Flow<Loadable<AddOperationSupervisor.State>> =
                    operation.currentJobFlow
                        .stickToFirstNotNull()
                        .distinctUntilChanged()
                        .flatMapLatest { job ->
                            job?.executionStateFlow?.mapToLoadable() ?: operation.prepare()
                        }

                f
            }.collectLoadableFlow()

        DialogOverlay(onDismissRequest = onClose) {
            LoadableGuard(
                repository,
                statusFlow,
            ) { repository, state ->
                contents(
                    repository.displayName,
                    state,
                    launchOptions,
                    onClose,
                )
            }
        }
    }
}

@Composable
private fun contents(
    archiveName: String,
    state: AddOperationSupervisor.State,
    launchOptions: MutableState<AddOperation.LaunchOptions>,
    onClose: () -> Unit = {},
) {
    val preparedResult = (state as? AddOperationSupervisor.Prepared)?.result
    val executeResult = (state as? AddOperationSupervisor.ExecutionState.Running)?.log ?: ""
    val isExecuting = state is AddOperationSupervisor.ExecutionState.Running

    val onTriggerExecute = {
        (state as AddOperationSupervisor.Prepared).launch(launchOptions.value)
    }

    DialogCardWithDialogInnerContainer(
        title =
            buildAnnotatedString {
                append("Update index of ")

                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(archiveName)
                }
            },
        content = {
            if (isExecuting || executeResult.isNotEmpty()) {
                Box(Modifier.verticalScroll(rememberScrollState())) {
                    Text(executeResult)
                }
            } else if (preparedResult != null) {
                val selectedFilenames =
                    remember {
                        derivedMutableStateOf(
                            onSet = { newValue ->
                                launchOptions.value =
                                    launchOptions.value.copy(
                                        addFilesSubsetLimit = newValue,
                                    )
                            },
                        ) {
                            launchOptions.value.addFilesSubsetLimit ?: emptySet()
                        }
                    }

                val selectedMoves =
                    remember {
                        derivedMutableStateOf(
                            onSet = { newValue ->
                                launchOptions.value =
                                    launchOptions.value.copy(
                                        movesSubsetLimit = newValue,
                                    )
                            },
                        ) {
                            launchOptions.value.movesSubsetLimit ?: emptySet()
                        }
                    }

                if (preparedResult.moves.isNotEmpty()) {
                    ItemManySelect(
                        "Moves",
                        allItemsLabel = { "All moves ($it)" },
                        itemLabel = { "${it.from} -> ${it.to}" },
                        allItems = preparedResult.moves,
                        selectedMoves,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (preparedResult.newFiles.isNotEmpty()) {
                    FileManySelect("New files", preparedResult.newFiles, selectedFilenames)
                }
            } else {
                Text("Preparing...")
            }
        },
        bottomContent = {
            DialogButtonContainer {
                var closeShown = false

                when (state) {
                    AddOperationSupervisor.ExecutionState.NotRunning -> {
                        DialogPrimaryButton("Execute index update", onClick = {}, enabled = false)
                    }
                    is AddOperationSupervisor.ExecutionState.Running -> {
                        if (state.finished) {
                            DialogDismissButton("Close", onClose)
                            closeShown = true
                        } else {
                            DialogPrimaryButton("Execute index update", onClick = {}, enabled = false)
                            Text("Running ...")
                        }
                    }
                    is AddOperationSupervisor.Prepared -> {
                        DialogPrimaryButton("Execute index update", onClick = onTriggerExecute, enabled = true)
                    }
                }

                if (!closeShown) {
                    Spacer(Modifier.weight(1f))
                    DialogDismissButton("Dismiss", onClose)
                }
            }
        },
    )
}

private val demo_preparation_result =
    AddOperation.PreparationResult(
        newFiles =
            listOf(
                "Documents/Something/There.pdf",
                "Photos/2024/04/photo_09.JPG",
            ),
        moves =
            listOf(
                Move("Document/bad-name.pdf", "Document/corrected-name.pdf"),
            ),
        missingFiles = emptyList(),
    )

@Preview
@Composable
private fun UpdateIndexOperationViewPreview() {
    DialogPreviewColumn {
        contents(
            archiveName = "Family Stuff",
            state =
                AddOperationSupervisor.Prepared(
                    demo_preparation_result,
                    {},
                ),
            launchOptions = mutableStateOf(AddOperation.LaunchOptions()),
        )

        contents(
            archiveName = "Family Stuff",
            state =
                AddOperationSupervisor.Prepared(
                    demo_preparation_result,
                    {},
                ),
            launchOptions = mutableStateOf(AddOperation.LaunchOptions()),
        )

        contents(
            archiveName = "Family Stuff",
            state =
                AddOperationSupervisor.ExecutionState.Running(
                    AddOperationSupervisor.AddProgress(setOf("Documents/Something/There.pdf"), emptyMap(), false),
                    AddOperationSupervisor.MoveProgress(emptySet(), emptyMap(), false),
                    "added: Documents/Something/There.pdf",
                ),
            launchOptions = mutableStateOf(AddOperation.LaunchOptions()),
        )

        contents(
            archiveName = "Family Stuff",
            AddOperationSupervisor.ExecutionState.Running(
                AddOperationSupervisor.AddProgress(setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG"), emptyMap(), false),
                AddOperationSupervisor.MoveProgress(emptySet(), emptyMap(), false),
                "added: Documents/Something/There.pdf\nadded: Photos/2024/04/photo_09.JPG",
            ),
            launchOptions = mutableStateOf(AddOperation.LaunchOptions()),
        )

        contents(
            archiveName = "Family Stuff",
            AddOperationSupervisor.ExecutionState.Running(
                AddOperationSupervisor.AddProgress(setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG"), emptyMap(), true),
                AddOperationSupervisor.MoveProgress(emptySet(), emptyMap(), false),
                "added: Documents/Something/There.pdf\nadded: Photos/2024/04/photo_09.JPG",
            ),
            launchOptions = mutableStateOf(AddOperation.LaunchOptions()),
        )
    }
}
