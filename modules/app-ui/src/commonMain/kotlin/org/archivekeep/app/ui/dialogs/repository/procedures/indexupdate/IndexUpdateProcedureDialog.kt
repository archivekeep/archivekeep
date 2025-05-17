package org.archivekeep.app.ui.dialogs.repository.procedures.indexupdate

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
import androidx.compose.ui.unit.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.procedures.add.IndexUpdateProcedureSupervisor
import org.archivekeep.app.core.procedures.add.IndexUpdateProcedureSupervisorService
import org.archivekeep.app.core.persistence.platform.demo.Documents
import org.archivekeep.app.core.persistence.platform.demo.LaptopSSD
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.utils.procedures.ProcedureExecutionState
import org.archivekeep.app.ui.components.base.layout.IntrinsicSizeWrapperLayout
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumn
import org.archivekeep.app.ui.components.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.dialog.fullWidthDialogWidthModifier
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState
import org.archivekeep.app.ui.components.feature.dialogs.operations.ExecutionErrorIfPresent
import org.archivekeep.app.ui.components.feature.dialogs.operations.toDialogOperationControlState
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectForRender
import org.archivekeep.app.ui.components.feature.operations.IndexUpdatePreparationProgress
import org.archivekeep.app.ui.components.feature.operations.LocalIndexUpdateProgress
import org.archivekeep.app.ui.components.feature.operations.ScrollableLogTextInDialog
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.domain.wiring.LocalIndexUpdateProcedureSupervisorService
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.app.ui.utils.stickToFirstNotNull
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.PreparationResult.Move
import org.archivekeep.files.procedures.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.waitLoadedValue
import org.jetbrains.compose.ui.tooling.preview.Preview

class IndexUpdateProcedureDialog(
    repositoryURI: RepositoryURI,
) : AbstractRepositoryDialog<IndexUpdateProcedureDialog.State, IndexUpdateProcedureDialog.VM>(repositoryURI) {
    data class State(
        val archiveName: String,
        // TODO: this should be Loadable - show loading of contents
        val operationState: IndexUpdateProcedureSupervisor.State,
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
                is IndexUpdateProcedureSupervisor.JobState ->
                    opState.state.toDialogOperationControlState(
                        onCancel = null,
                        onHide = onClose,
                        onClose = onClose,
                    )

                is IndexUpdateProcedureSupervisor.Preparation ->
                    DialogOperationControlState.NotRunning(
                        onLaunch = ::onTriggerExecute,
                        onClose = onClose,
                        canLaunch = opState.result is IndexUpdateProcedure.PreparationResult,
                    )
            }

        private fun onTriggerExecute() {
            (operationState as IndexUpdateProcedureSupervisor.Preparation).launch(
                IndexUpdateProcedure.LaunchOptions(
                    selectedFilesToAdd.value,
                    selectedMovesToExecute.value,
                ),
            )
        }

        override fun dialogWidthModifier(): Modifier = fullWidthDialogWidthModifier
    }

    inner class VM(
        val scope: CoroutineScope,
        val repository: SharedFlow<RepositoryInformation>,
        val indexUpdateProcedureSupervisorService: IndexUpdateProcedureSupervisorService,
        val _onClose: () -> Unit,
    ) : IVM {
        val operation = indexUpdateProcedureSupervisorService.getAddOperation(uri)

        @OptIn(ExperimentalCoroutinesApi::class)
        val operationStateFlow =
            operation.currentJobFlow
                .stickToFirstNotNull()
                .distinctUntilChanged()
                .flatMapLatest { job ->
                    job?.state?.mapToLoadable() ?: operation.prepare()
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
    ): VM {
        val addOperationSupervisorService = LocalIndexUpdateProcedureSupervisorService.current

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
    override fun rememberState(vm: VM): Loadable<State> =
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
            is IndexUpdateProcedureSupervisor.Preparation -> {
                when (val preparationState = operationState.result) {
                    is IndexUpdateProcedure.PreparationProgress -> {
                        LabelText("Preparing index update operation:")
                        IndexUpdatePreparationProgress(preparationState)
                    }

                    is IndexUpdateProcedure.PreparationResult -> {
                        val movesManySelect =
                            rememberManySelectForRender(
                                preparationState.moves,
                                state.selectedMovesToExecute,
                                "Moves",
                                allItemsLabel = { "All moves ($it)" },
                                itemLabelText = { "${it.from} -> ${it.to}" },
                            )
                        val filesManySelect =
                            rememberManySelectForRender(
                                preparationState.newFiles,
                                state.selectedFilesToAdd,
                                "New files",
                                allItemsLabel = { "All new files ($it)" },
                                itemLabelText = { it },
                            )

                        val guessedWidth = max(filesManySelect.guessedWidth, movesManySelect.guessedWidth)

                        IntrinsicSizeWrapperLayout(
                            minIntrinsicWidth = guessedWidth,
                            maxIntrinsicWidth = guessedWidth,
                        ) {
                            ScrollableLazyColumn {
                                if (preparationState.moves.isNotEmpty()) {
                                    movesManySelect.render(this)
                                    item { Spacer(Modifier.height(12.dp)) }
                                }

                                if (preparationState.newFiles.isNotEmpty()) {
                                    filesManySelect.render(this)
                                }
                            }
                        }
                    }
                }
            }

            is IndexUpdateProcedureSupervisor.JobState -> {
                LocalIndexUpdateProgress(
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
    state: IndexUpdateProcedureSupervisor.State,
    selectedFilesToAdd: MutableState<Set<String>>,
    selectedMovesToExecute: MutableState<Set<Move>>,
) {
    val DocumentsInLaptop = Documents.inStorage(LaptopSSD.reference).storageRepository

    val dialog = IndexUpdateProcedureDialog(DocumentsInLaptop.uri)

    dialog.renderDialogCard(
        IndexUpdateProcedureDialog.State(
            archiveName,
            state,
            selectedFilesToAdd,
            selectedMovesToExecute,
            onClose = {},
        ),
    )
}

private val demo_preparation_result =
    IndexUpdateProcedure.PreparationResult(
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
                IndexUpdateProcedureSupervisor.Preparation(
                    demo_preparation_result,
                    launch = {},
                ),
            selectedFilesToAdd = mutableStateOf(emptySet()),
            selectedMovesToExecute = mutableStateOf(emptySet()),
        )

        renderPreview(
            archiveName = "Family Stuff",
            state =
                IndexUpdateProcedureSupervisor.Preparation(
                    demo_preparation_result,
                    launch = {},
                ),
            selectedFilesToAdd = mutableStateOf(emptySet()),
            selectedMovesToExecute = mutableStateOf(emptySet()),
        )

        renderPreview(
            archiveName = "Family Stuff",
            state =
                IndexUpdateProcedureSupervisor.JobState(
                    IndexUpdateAddProgress(setOf("Documents/Something/There.pdf"), setOf("Documents/Something/There.pdf"), emptyMap(), false),
                    IndexUpdateMoveProgress(emptySet(),  emptySet(), emptyMap(), false),
                    "added: Documents/Something/There.pdf",
                    ProcedureExecutionState.Running,
                ),
            selectedFilesToAdd = mutableStateOf(emptySet()),
            selectedMovesToExecute = mutableStateOf(emptySet()),
        )

        renderPreview(
            archiveName = "Family Stuff",
            IndexUpdateProcedureSupervisor.JobState(
                IndexUpdateAddProgress(setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG"), setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG"), emptyMap(), false),
                IndexUpdateMoveProgress(emptySet(),  emptySet(), emptyMap(), false),
                "added: Documents/Something/There.pdf\nadded: Photos/2024/04/photo_09.JPG",
                ProcedureExecutionState.Running,
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
            IndexUpdateProcedureSupervisor.JobState(
                IndexUpdateAddProgress(setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG", "Photos/2024/04/photo_10.JPG"), setOf("Documents/Something/There.pdf", "Photos/2024/04/photo_09.JPG"), emptyMap(), true),
                IndexUpdateMoveProgress(emptySet(), emptySet(), emptyMap(), false),
                "added: Documents/Something/There.pdf\nadded: Photos/2024/04/photo_09.JPG",
                ProcedureExecutionState.Running,
            ),
            selectedFilesToAdd = mutableStateOf(emptySet()),
            selectedMovesToExecute = mutableStateOf(emptySet()),
        )
    }
}
