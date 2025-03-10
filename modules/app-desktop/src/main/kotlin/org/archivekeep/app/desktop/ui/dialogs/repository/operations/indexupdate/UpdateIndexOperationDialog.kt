package org.archivekeep.app.desktop.ui.dialogs.repository.operations.indexupdate

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.operations.add.AddOperationSupervisor
import org.archivekeep.app.core.operations.add.AddOperationSupervisorService
import org.archivekeep.app.core.persistence.platform.demo.Documents
import org.archivekeep.app.core.persistence.platform.demo.LaptopSSD
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalAddOperationSupervisorService
import org.archivekeep.app.desktop.ui.components.FileManySelect
import org.archivekeep.app.desktop.ui.components.ItemManySelect
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.desktop.ui.utils.appendBoldSpan
import org.archivekeep.app.desktop.utils.collectAsLoadable
import org.archivekeep.app.desktop.utils.derivedMutableStateOf
import org.archivekeep.app.desktop.utils.stickToFirstNotNull
import org.archivekeep.files.operations.AddOperation
import org.archivekeep.files.operations.AddOperation.PreparationResult.Move
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.waitLoadedValue

class UpdateIndexOperationDialog(
    repositoryURI: RepositoryURI,
) : AbstractRepositoryDialog<UpdateIndexOperationDialog.State, UpdateIndexOperationDialog.VM>(repositoryURI) {
    data class State(
        val archiveName: String,
        // TODO: this should be Loadable - show loading of contents
        val operationState: AddOperationSupervisor.State,
        val launchOptions: MutableState<AddOperation.LaunchOptions>,
        val onClose: () -> Unit,
    ) : IState {
        override val title =
            buildAnnotatedString {
                appendBoldSpan(archiveName)
                append(" - index update")
            }

        val preparedResult = (operationState as? AddOperationSupervisor.Prepared)?.result
        val executeResult = (operationState as? AddOperationSupervisor.ExecutionState.Running)?.log ?: ""
        val isExecuting = operationState is AddOperationSupervisor.ExecutionState.Running
    }

    inner class VM(
        val scope: CoroutineScope,
        val repository: SharedFlow<RepositoryInformation>,
        val addOperationSupervisorService: AddOperationSupervisorService,
        val _onClose: () -> Unit,
    ) : IVM {
        val operation = addOperationSupervisorService.getAddOperation(uri)

        @OptIn(ExperimentalCoroutinesApi::class)
        val operationStateFlow =
            operation.currentJobFlow
                .stickToFirstNotNull()
                .distinctUntilChanged()
                .flatMapLatest { job ->
                    job?.executionStateFlow?.mapToLoadable() ?: operation.prepare()
                }

        val launchOptions = mutableStateOf(AddOperation.LaunchOptions(addFilesSubsetLimit = emptySet(), movesSubsetLimit = emptySet()))

        override fun onClose() {
            _onClose()
        }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        repository: Repository,
        onClose: () -> Unit,
    ): UpdateIndexOperationDialog.VM {
        val addOperationSupervisorService = LocalAddOperationSupervisorService.current

        return remember {
            VM(
                scope,
                repository.informationFlow,
                addOperationSupervisorService,
                _onClose = onClose,
            )
        }
    }

    @Composable
    override fun rememberState(vm: UpdateIndexOperationDialog.VM): Loadable<State> =
        remember(vm) {
            combine(
                vm.repository,
                vm.operationStateFlow.waitLoadedValue(),
            ) { repository, operationState ->
                State(
                    archiveName = repository.displayName,
                    operationState = operationState,
                    vm.launchOptions,
                    onClose = vm::onClose,
                )
            }
        }.collectAsLoadable()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        if (state.isExecuting || state.executeResult.isNotEmpty()) {
            Box(Modifier.verticalScroll(rememberScrollState())) {
                Text(state.executeResult)
            }
        } else if (state.preparedResult != null) {
            val selectedFilenames =
                remember {
                    derivedMutableStateOf(
                        onSet = { newValue ->
                            state.launchOptions.value =
                                state.launchOptions.value.copy(
                                    addFilesSubsetLimit = newValue,
                                )
                        },
                    ) {
                        state.launchOptions.value.addFilesSubsetLimit ?: emptySet()
                    }
                }

            val selectedMoves =
                remember {
                    derivedMutableStateOf(
                        onSet = { newValue ->
                            state.launchOptions.value =
                                state.launchOptions.value.copy(
                                    movesSubsetLimit = newValue,
                                )
                        },
                    ) {
                        state.launchOptions.value.movesSubsetLimit ?: emptySet()
                    }
                }

            if (state.preparedResult.moves.isNotEmpty()) {
                ItemManySelect(
                    "Moves",
                    allItemsLabel = { "All moves ($it)" },
                    itemLabel = { "${it.from} -> ${it.to}" },
                    allItems = state.preparedResult.moves,
                    selectedMoves,
                )
                Spacer(Modifier.height(12.dp))
            }

            if (state.preparedResult.newFiles.isNotEmpty()) {
                FileManySelect("New files", state.preparedResult.newFiles, selectedFilenames)
            }
        } else {
            Text("Preparing...")
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        val onTriggerExecute = {
            (state.operationState as AddOperationSupervisor.Prepared).launch(state.launchOptions.value)
        }

        var closeShown = false

        when (val opState = state.operationState) {
            AddOperationSupervisor.ExecutionState.NotRunning -> {
                DialogPrimaryButton("Execute index update", onClick = {}, enabled = false)
            }

            is AddOperationSupervisor.ExecutionState.Running -> {
                if (opState.finished) {
                    DialogDismissButton("Close", state.onClose)
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
            DialogDismissButton("Dismiss", state.onClose)
        }
    }
}

@Composable
private fun renderPreview(
    archiveName: String,
    state: AddOperationSupervisor.State,
    launchOptions: MutableState<AddOperation.LaunchOptions>,
) {
    val DocumentsInLaptop = Documents.inStorage(LaptopSSD.reference).storageRepository

    val dialog = UpdateIndexOperationDialog(DocumentsInLaptop.uri)

    dialog.renderDialogCard(
        UpdateIndexOperationDialog.State(
            archiveName,
            state,
            launchOptions,
            onClose = {},
        ),
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
        renderPreview(
            archiveName = "Family Stuff",
            state =
                AddOperationSupervisor.Prepared(
                    demo_preparation_result,
                    {},
                ),
            launchOptions = mutableStateOf(AddOperation.LaunchOptions()),
        )

        renderPreview(
            archiveName = "Family Stuff",
            state =
                AddOperationSupervisor.Prepared(
                    demo_preparation_result,
                    {},
                ),
            launchOptions = mutableStateOf(AddOperation.LaunchOptions()),
        )

        renderPreview(
            archiveName = "Family Stuff",
            state =
                AddOperationSupervisor.ExecutionState.Running(
                    AddOperationSupervisor.AddProgress(setOf("Documents/Something/There.pdf"), emptyMap(), false),
                    AddOperationSupervisor.MoveProgress(emptySet(), emptyMap(), false),
                    "added: Documents/Something/There.pdf",
                ),
            launchOptions = mutableStateOf(AddOperation.LaunchOptions()),
        )

        renderPreview(
            archiveName = "Family Stuff",
            AddOperationSupervisor.ExecutionState.Running(
                AddOperationSupervisor.AddProgress(setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG"), emptyMap(), false),
                AddOperationSupervisor.MoveProgress(emptySet(), emptyMap(), false),
                "added: Documents/Something/There.pdf\nadded: Photos/2024/04/photo_09.JPG",
            ),
            launchOptions = mutableStateOf(AddOperation.LaunchOptions()),
        )

        renderPreview(
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
