package org.archivekeep.app.ui.dialogs.repository.procedures.deletedcleanup

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.procedures.deletedcleanup.DeletedFilesCleanupProcedureSupervisor
import org.archivekeep.app.core.procedures.deletedcleanup.DeletedFilesCleanupProcedureSupervisorService
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumnWithGuessedWidth
import org.archivekeep.app.ui.components.designsystem.dialog.fullWidthDialogWidthModifier
import org.archivekeep.app.ui.components.designsystem.elements.InfoAlert
import org.archivekeep.app.ui.components.designsystem.sections.SectionCardSecondaryButton
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState
import org.archivekeep.app.ui.components.feature.dialogs.operations.toDialogOperationControlState
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectForRender
import org.archivekeep.app.ui.components.feature.operations.DeletedFilesCleanupProgress
import org.archivekeep.app.ui.components.feature.operations.OperationProgressTabs
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.domain.wiring.LocalArchiveOperationLaunchers
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.app.ui.utils.stickToFirstNotNull
import org.archivekeep.files.procedures.deletedcleanup.DeletedFilesCleanupProcedure
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.cleanup_moved_or_renamed
import org.archivekeep.ui.resources.cleanup_proceed_with_addition
import org.archivekeep.ui.resources.dialog_reindex_title
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.mapIfLoadedOrNull
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.loading.waitLoadedValue
import org.jetbrains.compose.resources.stringResource

class DeletedFilesCleanupProcedureDialog(
    repositoryURI: RepositoryURI,
) : AbstractRepositoryDialog<DeletedFilesCleanupProcedureDialog.State, DeletedFilesCleanupProcedureDialog.VM>(repositoryURI) {
    data class State(
        val archiveName: String,
        // TODO: this should be Loadable - show loading of contents
        val operationState: DeletedFilesCleanupProcedureSupervisor.State,
        val selectedFilesToReindex: MutableState<Set<String>>,
        val canAdd: Boolean,
        val onClose: () -> Unit,
    ) : IState {
        override val title =
            buildAnnotatedString {
                appendBoldSpan(archiveName)
                append(" - remove deleted files from index")
            }

        val controlState: DialogOperationControlState =
            when (val opState = operationState) {
                is DeletedFilesCleanupProcedureSupervisor.JobState -> {
                    opState.executionState.toDialogOperationControlState(
                        onCancel = null,
                        onHide = onClose,
                        onClose = onClose,
                    )
                }

                is DeletedFilesCleanupProcedureSupervisor.Preparation -> {
                    DialogOperationControlState.NotRunning(
                        onLaunch = ::onTriggerExecute,
                        onClose = onClose,
                        canLaunch = true,
                    )
                }
            }

        private fun onTriggerExecute() {
            (operationState as DeletedFilesCleanupProcedureSupervisor.Preparation).launch(
                DeletedFilesCleanupProcedure.LaunchOptions(
                    selectedFilesToReindex.value,
                ),
            )
        }

        override fun dialogWidthModifier(): Modifier = fullWidthDialogWidthModifier
    }

    inner class VM(
        val scope: CoroutineScope,
        val repository: SharedFlow<RepositoryInformation>,
        val deletedFilesCleanupProcedureSupervisorService: DeletedFilesCleanupProcedureSupervisorService,
        val canAddFlow: Flow<OptionalLoadable<Boolean>>,
        val _onClose: () -> Unit,
    ) : IVM {
        val operation = deletedFilesCleanupProcedureSupervisorService.getDeletedFilesCleanupOperation(uri)

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
        val deletedFilesCleanupProcedureSupervisorService = LocalApplicationServices.current.deletedFilesCleanupProcedureSupervisorService
        val canAddFlow =
            remember(repository) {
                repository.localRepoStatus.mapLoadedData { it.summary.canAdd }
            }

        return remember {
            VM(
                scope,
                repository.informationFlow,
                deletedFilesCleanupProcedureSupervisorService,
                canAddFlow = canAddFlow,
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
                vm.canAddFlow,
            ) { repository, operationState, canAdd ->
                State(
                    archiveName = repository.displayName,
                    operationState = operationState,
                    selectedFilesToReindex = mutableStateOf(emptySet()),
                    canAdd = canAdd.mapIfLoadedOrNull { it } ?: false,
                    onClose = vm::onClose,
                )
            }
        }.collectAsLoadable()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        when (val operationState = state.operationState) {
            is DeletedFilesCleanupProcedureSupervisor.Preparation -> {
                val preparationState = operationState.result

                val filesManySelect =
                    rememberManySelectForRender(
                        preparationState.missingFiles,
                        state.selectedFilesToReindex,
                        "Deleted files to remove from index",
                        allItemsLabel = { "All missing/deleted files ($it)" },
                        itemLabelText = { it },
                    )

                if (state.canAdd) {
                    val launchers = LocalArchiveOperationLaunchers.current

                    InfoAlert {
                        Text(stringResource(Res.string.cleanup_moved_or_renamed))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(Res.string.cleanup_proceed_with_addition))
                        Spacer(Modifier.height(8.dp))
                        SectionCardSecondaryButton(
                            onClick = {
                                launchers.openIndexUpdateOperation(uri)
                                state.onClose()
                            },
                            text = stringResource(Res.string.dialog_reindex_title),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                ScrollableLazyColumnWithGuessedWidth(filesManySelect.guessedWidth) {
                    if (preparationState.missingFiles.isNotEmpty()) {
                        filesManySelect.render(this)
                    }
                }
            }

            is DeletedFilesCleanupProcedureSupervisor.JobState -> {
                OperationProgressTabs(::DeletedFilesCleanupProgress, operationState)
            }
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        DialogOperationControlButtons(
            state.controlState,
            launchText = "Execute removal from index",
        )
    }
}
