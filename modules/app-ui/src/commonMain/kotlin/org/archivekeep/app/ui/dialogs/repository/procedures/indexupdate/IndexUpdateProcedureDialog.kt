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
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumnWithGuessedWidth
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.dialog.fullWidthDialogWidthModifier
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState
import org.archivekeep.app.ui.components.feature.dialogs.operations.toDialogOperationControlState
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectForRender
import org.archivekeep.app.ui.components.feature.operations.IndexUpdatePreparationProgress
import org.archivekeep.app.ui.components.feature.operations.LocalIndexUpdateProgress
import org.archivekeep.app.ui.components.feature.operations.OperationProgressTabs
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.app.ui.utils.stickToFirstNotNull
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure.PreparationResult.Move
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_preparing
import org.archivekeep.ui.resources.dialog_index_update_title
import org.archivekeep.ui.resources.manyselect_moves
import org.archivekeep.ui.resources.manyselect_moves_all
import org.archivekeep.ui.resources.manyselect_new_files
import org.archivekeep.ui.resources.manyselect_new_files_all
import org.archivekeep.ui.resources.procedure_execute_index_update
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.waitLoadedValue
import org.jetbrains.compose.resources.stringResource

class IndexUpdateProcedureDialog(
    repositoryURI: RepositoryURI,
) : AbstractRepositoryDialog<IndexUpdateProcedureDialog.State, IndexUpdateProcedureDialog.VM>(repositoryURI) {
    data class State(
        val archiveName: String,
        // TODO: this should be Loadable - show loading of contents
        val operationState: IndexUpdateProcedureSupervisor.State,
        val selectedFilesToAdd: MutableState<Set<String>>,
        val selectedMovesToExecute: MutableState<Set<Move>>,
        val onCancel: () -> Unit,
        val onClose: () -> Unit,
    ) {
        val controlState: DialogOperationControlState =
            when (val opState = operationState) {
                is IndexUpdateProcedureSupervisor.JobState -> {
                    opState.executionState.toDialogOperationControlState(
                        onCancel = onCancel,
                        onHide = onClose,
                        onClose = onClose,
                    )
                }

                is IndexUpdateProcedureSupervisor.Preparation -> {
                    DialogOperationControlState.NotRunning(
                        onLaunch = ::onTriggerExecute,
                        onClose = onClose,
                        canLaunch = opState.result is IndexUpdateProcedure.PreparationResult,
                    )
                }
            }

        private fun onTriggerExecute() {
            (operationState as IndexUpdateProcedureSupervisor.Preparation).launch(
                IndexUpdateProcedure.LaunchOptions(
                    selectedFilesToAdd.value,
                    selectedMovesToExecute.value,
                ),
            )
        }

    }

    override fun dialogWidthModifier(state: State) = fullWidthDialogWidthModifier

    @Composable
    override fun title(state: State) =
        buildAnnotatedString {
            appendBoldSpan(state.archiveName)
            append(" - ")
            append(stringResource(Res.string.dialog_index_update_title))
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
        val addOperationSupervisorService = LocalApplicationServices.current.addOperationSupervisorService

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
                    onCancel = vm.operation::cancel,
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
                        LabelText(stringResource(Res.string.common_preparing))
                        IndexUpdatePreparationProgress(preparationState)
                    }

                    is IndexUpdateProcedure.PreparationResult -> {
                        val movesManySelect =
                            rememberManySelectForRender(
                                preparationState.moves,
                                state.selectedMovesToExecute,
                                stringResource(Res.string.manyselect_moves),
                                allItemsLabel = { stringResource(Res.string.manyselect_moves_all, it.toString()) },
                                itemLabelText = { "${it.from} -> ${it.to}" },
                            )
                        val filesManySelect =
                            rememberManySelectForRender(
                                preparationState.newFileNames,
                                state.selectedFilesToAdd,
                                stringResource(Res.string.manyselect_new_files),
                                allItemsLabel = { stringResource(Res.string.manyselect_new_files_all, it.toString()) },
                                itemLabelText = { it },
                            )

                        val guessedWidth = max(filesManySelect.guessedWidth, movesManySelect.guessedWidth)

                        ScrollableLazyColumnWithGuessedWidth(guessedWidth) {
                            if (preparationState.moves.isNotEmpty()) {
                                movesManySelect.render(this)
                                item { Spacer(Modifier.height(12.dp)) }
                            }

                            if (preparationState.newFileNames.isNotEmpty()) {
                                filesManySelect.render(this)
                            }
                        }
                    }
                }
            }

            is IndexUpdateProcedureSupervisor.JobState -> {
                OperationProgressTabs(::LocalIndexUpdateProgress, operationState)
            }
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        DialogOperationControlButtons(
            state.controlState,
            launchText = stringResource(Res.string.procedure_execute_index_update),
        )
    }
}
