package org.archivekeep.app.ui.dialogs.repository.procedures.verify

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import org.archivekeep.app.core.procedures.verify.VerifySupervisor
import org.archivekeep.app.core.procedures.verify.VerifySupervisorService
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.designsystem.dialog.fullWidthDialogWidthModifier
import org.archivekeep.app.ui.components.designsystem.elements.InfoAlert
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState
import org.archivekeep.app.ui.components.feature.dialogs.operations.toDialogOperationControlState
import org.archivekeep.app.ui.components.feature.operations.OperationProgressTabs
import org.archivekeep.app.ui.components.feature.operations.VerifyProgress
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.app.ui.utils.stickToFirstNotNull
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.dialog_verify_files_to_check
import org.archivekeep.ui.resources.dialog_verify_info
import org.archivekeep.ui.resources.dialog_verify_title
import org.archivekeep.ui.resources.procedure_execute_verify
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.waitLoadedValue
import org.jetbrains.compose.resources.stringResource

class VerifyContentsDialog(
    repositoryURI: RepositoryURI,
) : AbstractRepositoryDialog<VerifyContentsDialog.State, VerifyContentsDialog.VM>(repositoryURI) {
    data class State(
        val archiveName: String,
        val operationState: VerifySupervisor.State,
        val onClose: () -> Unit,
    ) {
        val controlState: DialogOperationControlState =
            when (val opState = operationState) {
                is VerifySupervisor.JobState -> {
                    opState.executionState.toDialogOperationControlState(
                        onCancel = null,
                        onHide = onClose,
                        onClose = onClose,
                    )
                }

                is VerifySupervisor.Preparation -> {
                    DialogOperationControlState.NotRunning(
                        onLaunch = ::onTriggerExecute,
                        onClose = onClose,
                        canLaunch = true,
                    )
                }
            }

        private fun onTriggerExecute() {
            (operationState as VerifySupervisor.Preparation).launch()
        }
    }

    override fun dialogWidthModifier(state: State) = fullWidthDialogWidthModifier

    @Composable
    override fun title(state: State) =
        buildAnnotatedString {
            appendBoldSpan(state.archiveName)
            append(" - ")
            append(stringResource(Res.string.dialog_verify_title))
        }

    inner class VM(
        val scope: CoroutineScope,
        val repository: SharedFlow<RepositoryInformation>,
        val verifySupervisorService: VerifySupervisorService,
        val _onClose: () -> Unit,
    ) : IVM {
        val operation = verifySupervisorService.getVerifyOperation(uri)

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
        val verifySupervisorService = LocalApplicationServices.current.verifySupervisorService

        return remember {
            VM(
                scope,
                repository.informationFlow,
                verifySupervisorService,
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
                    onClose = vm::onClose,
                )
            }
        }.collectAsLoadable()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        when (val operationState = state.operationState) {
            is VerifySupervisor.Preparation -> {
                val preparationState = operationState.result
                Text(
                    stringResource(
                        Res.string.dialog_verify_files_to_check,
                        preparationState.filesToVerify.size,
                    ),
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoAlert {
                    Text(stringResource(Res.string.dialog_verify_info))
                }
            }

            is VerifySupervisor.JobState -> {
                OperationProgressTabs(::VerifyProgress, operationState)
            }
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        DialogOperationControlButtons(
            state.controlState,
            launchText = stringResource(Res.string.procedure_execute_verify),
        )
    }
}
