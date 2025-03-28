package org.archivekeep.app.desktop.ui.dialogs.repository.operations.indexupdate

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import org.archivekeep.app.core.utils.operations.OperationExecutionState
import org.archivekeep.app.desktop.domain.wiring.LocalAddOperationSupervisorService
import org.archivekeep.app.desktop.ui.components.FileManySelect
import org.archivekeep.app.desktop.ui.components.ItemManySelect
import org.archivekeep.app.desktop.ui.components.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.desktop.ui.components.dialogs.operations.DialogOperationControlState
import org.archivekeep.app.desktop.ui.components.dialogs.operations.ExecutionErrorIfPresent
import org.archivekeep.app.desktop.ui.components.dialogs.operations.toDialogOperationControlState
import org.archivekeep.app.desktop.ui.components.operations.IndexUpdatePreparationProgress
import org.archivekeep.app.desktop.ui.components.operations.LocalIndexUpdateProgress
import org.archivekeep.app.desktop.ui.components.operations.ScrollableLogTextInDialog
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.dialog.LabelText
import org.archivekeep.app.desktop.ui.designsystem.layout.scrollable.ScrollableColumn
import org.archivekeep.app.desktop.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.desktop.ui.utils.appendBoldSpan
import org.archivekeep.app.desktop.utils.collectAsLoadable
import org.archivekeep.app.desktop.utils.stickToFirstNotNull
import org.archivekeep.files.operations.indexupdate.AddOperation
import org.archivekeep.files.operations.indexupdate.AddOperation.PreparationResult.Move
import org.archivekeep.files.operations.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.operations.indexupdate.IndexUpdateMoveProgress
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
        val selectedFilesToAdd: MutableState<Set<String>>,
        val selectedMovesToExecute: MutableState<Set<Move>>,
        val onClose: () -> Unit,
    ) : IState {
        override val title =
            buildAnnotatedString {
                appendBoldSpan(archiveName)
                append(" - index update")
            }

        val controlState: DialogOperationControlState =
            when (val opState = operationState) {
                is AddOperationSupervisor.JobState ->
                    opState.state.toDialogOperationControlState(
                        onCancel = null,
                        onHide = onClose,
                        onClose = onClose,
                    )
                is AddOperationSupervisor.Preparation ->
                    DialogOperationControlState.NotRunning(
                        onLaunch = ::onTriggerExecute,
                        onClose = onClose,
                        canLaunch = opState.result is AddOperation.PreparationResult,
                    )
            }

        private fun onTriggerExecute() {
            (operationState as AddOperationSupervisor.Preparation).launch(
                AddOperation.LaunchOptions(
                    selectedFilesToAdd.value,
                    selectedMovesToExecute.value,
                ),
            )
        }
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
                    selectedFilesToAdd = mutableStateOf(emptySet()),
                    selectedMovesToExecute = mutableStateOf(emptySet()),
                    onClose = vm::onClose,
                )
            }
        }.collectAsLoadable()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        when (val operationState = state.operationState) {
            is AddOperationSupervisor.Preparation -> {
                ScrollableColumn {
                    when (val preparationState = operationState.result) {
                        is AddOperation.PreparationProgress -> {
                            LabelText("Preparing index update operation:")
                            IndexUpdatePreparationProgress(preparationState)
                        }

                        is AddOperation.PreparationResult -> {
                            if (preparationState.moves.isNotEmpty()) {
                                ItemManySelect(
                                    "Moves",
                                    allItemsLabel = { "All moves ($it)" },
                                    itemLabelText = { "${it.from} -> ${it.to}" },
                                    allItems = preparationState.moves,
                                    state.selectedMovesToExecute,
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            if (preparationState.newFiles.isNotEmpty()) {
                                FileManySelect("New files", preparationState.newFiles, state.selectedFilesToAdd)
                            }
                        }
                    }
                }
            }
            is AddOperationSupervisor.JobState -> {
                LocalIndexUpdateProgress(
                    operationState.movesToExecute,
                    operationState.filesToAdd,
                    operationState.moveProgress,
                    operationState.addProgress,
                )
                Spacer(Modifier.height(4.dp))
                ScrollableLogTextInDialog(operationState.log)
                ExecutionErrorIfPresent(operationState.state)
            }
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        DialogOperationControlButtons(
            state.controlState,
            launchText = "Execute index update",
        )
    }
}

@Composable
private fun renderPreview(
    archiveName: String,
    state: AddOperationSupervisor.State,
    selectedFilesToAdd: MutableState<Set<String>>,
    selectedMovesToExecute: MutableState<Set<Move>>,
) {
    val DocumentsInLaptop = Documents.inStorage(LaptopSSD.reference).storageRepository

    val dialog = UpdateIndexOperationDialog(DocumentsInLaptop.uri)

    dialog.renderDialogCard(
        UpdateIndexOperationDialog.State(
            archiveName,
            state,
            selectedFilesToAdd,
            selectedMovesToExecute,
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
        errorFiles = emptyMap(),
    )

@Preview
@Composable
private fun UpdateIndexOperationViewPreview() {
    DialogPreviewColumn {
        renderPreview(
            archiveName = "Family Stuff",
            state =
                AddOperationSupervisor.Preparation(
                    demo_preparation_result,
                    launch = {},
                ),
            selectedFilesToAdd = mutableStateOf(emptySet()),
            selectedMovesToExecute = mutableStateOf(emptySet()),
        )

        renderPreview(
            archiveName = "Family Stuff",
            state =
                AddOperationSupervisor.Preparation(
                    demo_preparation_result,
                    launch = {},
                ),
            selectedFilesToAdd = mutableStateOf(emptySet()),
            selectedMovesToExecute = mutableStateOf(emptySet()),
        )

        renderPreview(
            archiveName = "Family Stuff",
            state =
                AddOperationSupervisor.JobState(
                    emptySet(),
                    setOf("Documents/Something/There.pdf"),
                    IndexUpdateAddProgress(setOf("Documents/Something/There.pdf"), emptyMap(), false),
                    IndexUpdateMoveProgress(emptySet(), emptyMap(), false),
                    "added: Documents/Something/There.pdf",
                    OperationExecutionState.Running,
                ),
            selectedFilesToAdd = mutableStateOf(emptySet()),
            selectedMovesToExecute = mutableStateOf(emptySet()),
        )

        renderPreview(
            archiveName = "Family Stuff",
            AddOperationSupervisor.JobState(
                emptySet(),
                setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG"),
                IndexUpdateAddProgress(setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG"), emptyMap(), false),
                IndexUpdateMoveProgress(emptySet(), emptyMap(), false),
                "added: Documents/Something/There.pdf\nadded: Photos/2024/04/photo_09.JPG",
                OperationExecutionState.Running,
            ),
            selectedFilesToAdd = mutableStateOf(emptySet()),
            selectedMovesToExecute = mutableStateOf(emptySet()),
        )
    }
}

@Preview
@Composable
private fun UpdateIndexOperationViewPreview2() {
    DialogPreviewColumn {
        renderPreview(
            archiveName = "Family Stuff",
            AddOperationSupervisor.JobState(
                emptySet(),
                setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG", "Photos/2024/04/photo_10.JPG"),
                IndexUpdateAddProgress(setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG"), emptyMap(), true),
                IndexUpdateMoveProgress(emptySet(), emptyMap(), false),
                "added: Documents/Something/There.pdf\nadded: Photos/2024/04/photo_09.JPG",
                OperationExecutionState.Running,
            ),
            selectedFilesToAdd = mutableStateOf(emptySet()),
            selectedMovesToExecute = mutableStateOf(emptySet()),
        )
    }
}
