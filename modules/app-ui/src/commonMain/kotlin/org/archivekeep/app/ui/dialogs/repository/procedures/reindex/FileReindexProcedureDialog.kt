package org.archivekeep.app.ui.dialogs.repository.procedures.reindex

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
import org.archivekeep.app.core.procedures.reindex.FileReindexProcedureSupervisor
import org.archivekeep.app.core.procedures.reindex.FileReindexProcedureSupervisorService
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.base.layout.IntrinsicSizeWrapperLayout
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumn
import org.archivekeep.app.ui.components.designsystem.dialog.fullWidthDialogWidthModifier
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState
import org.archivekeep.app.ui.components.feature.dialogs.operations.ExecutionErrorIfPresent
import org.archivekeep.app.ui.components.feature.dialogs.operations.toDialogOperationControlState
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectForRender
import org.archivekeep.app.ui.components.feature.operations.FileReindexProgress
import org.archivekeep.app.ui.components.feature.operations.ScrollableLogTextInDialog
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.app.ui.utils.stickToFirstNotNull
import org.archivekeep.files.procedures.reindex.FileReindexProcedure
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.waitLoadedValue

class FileReindexProcedureDialog(
    repositoryURI: RepositoryURI,
) : AbstractRepositoryDialog<FileReindexProcedureDialog.State, FileReindexProcedureDialog.VM>(repositoryURI) {
    data class State(
        val archiveName: String,
        // TODO: this should be Loadable - show loading of contents
        val operationState: FileReindexProcedureSupervisor.State,
        val selectedFilesToReindex: MutableState<Set<String>>,
        val onClose: () -> Unit,
    ) : IState {
        override val title =
            buildAnnotatedString {
                appendBoldSpan(archiveName)
                append(" - reindex changed files")
            }

        val controlState: DialogOperationControlState =
            when (val opState = operationState) {
                is FileReindexProcedureSupervisor.JobState -> {
                    opState.state.toDialogOperationControlState(
                        onCancel = null,
                        onHide = onClose,
                        onClose = onClose,
                    )
                }

                is FileReindexProcedureSupervisor.Preparation -> {
                    DialogOperationControlState.NotRunning(
                        onLaunch = ::onTriggerExecute,
                        onClose = onClose,
                        canLaunch = true,
                    )
                }
            }

        private fun onTriggerExecute() {
            (operationState as FileReindexProcedureSupervisor.Preparation).launch(
                FileReindexProcedure.LaunchOptions(
                    selectedFilesToReindex.value,
                ),
            )
        }

        override fun dialogWidthModifier(): Modifier = fullWidthDialogWidthModifier
    }

    inner class VM(
        val scope: CoroutineScope,
        val repository: SharedFlow<RepositoryInformation>,
        val fileReindexProcedureSupervisorService: FileReindexProcedureSupervisorService,
        val _onClose: () -> Unit,
    ) : IVM {
        val operation = fileReindexProcedureSupervisorService.getFileReindexOperation(uri)

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
        val fileReindexProcedureSupervisorService = LocalApplicationServices.current.fileReindexProcedureSupervisorService

        return remember {
            VM(
                scope,
                repository.informationFlow,
                fileReindexProcedureSupervisorService,
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
                    selectedFilesToReindex = mutableStateOf(emptySet()),
                    onClose = vm::onClose,
                )
            }
        }.collectAsLoadable()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        when (val operationState = state.operationState) {
            is FileReindexProcedureSupervisor.Preparation -> {
                val preparationState = operationState.result

                val filesManySelect =
                    rememberManySelectForRender(
                        preparationState.modifiedIndexedFiles,
                        state.selectedFilesToReindex,
                        "Modified files",
                        allItemsLabel = { "All modified files ($it)" },
                        itemLabelText = { it },
                    )

                val guessedWidth = filesManySelect.guessedWidth

                IntrinsicSizeWrapperLayout(
                    minIntrinsicWidth = guessedWidth,
                    maxIntrinsicWidth = guessedWidth,
                ) {
                    ScrollableLazyColumn {
                        if (preparationState.modifiedIndexedFiles.isNotEmpty()) {
                            filesManySelect.render(this)
                        }
                    }
                }
            }

            is FileReindexProcedureSupervisor.JobState -> {
                FileReindexProgress(
                    operationState.reindexProgress,
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
