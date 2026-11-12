package org.archivekeep.app.ui.dialogs.repository.procedures.sync

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.procedures.sync.RepoToRepoSyncService
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.designsystem.dialog.fullWidthDialogWidthModifier
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.ui.dialogs.AbstractDialog
import org.archivekeep.app.ui.dialogs.repository.procedures.sync.parts.RepoToRepoSyncMainContents
import org.archivekeep.app.ui.domain.wiring.LocalApplicationServices
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.dialog_copy_to_title
import org.archivekeep.ui.resources.dialog_upload_message
import org.archivekeep.ui.resources.dialog_upload_message_with_repo
import org.archivekeep.utils.loading.Loadable
import org.jetbrains.compose.resources.stringResource

data class UploadToRepoDialog(
    val repositoryURI: RepositoryURI,
    val from: RepositoryURI,
) : AbstractDialog<UploadToRepoDialog.State, UploadToRepoDialog.VM>() {
    data class State(
        val targetRepository: StorageRepository,
        val sourceRepository: StorageRepository,
        val userFlowState: RepoToRepoSyncUserFlow.State,
        val onLaunch: () -> Unit,
        val onCancel: () -> Unit,
        val onClose: () -> Unit,
    )

    override fun dialogWidthModifier(state: State) = fullWidthDialogWidthModifier

    @Composable
    override fun title(state: State) =
        buildAnnotatedString {
            appendBoldSpan(state.sourceRepository.displayName)
            append(" - ")
            append(stringResource(Res.string.dialog_copy_to_title))
        }

    inner class VM(
        val storageService: StorageService,
        repoToRepoSyncService: RepoToRepoSyncService,
        val scope: CoroutineScope,
        val _onClose: () -> Unit,
    ) : IVM {
        val sync = repoToRepoSyncService.getRepoToRepoSync(from, repositoryURI)
        val userFlow = RepoToRepoSyncUserFlow(scope, sync)

        override fun onClose() {
            _onClose()
        }
    }

    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        onClose: () -> Unit,
    ): VM {
        val storageService = LocalApplicationServices.current.storageService
        val syncService = LocalApplicationServices.current.syncService

        return remember(storageService, syncService, scope) {
            VM(storageService, syncService, scope, onClose)
        }
    }

    @Composable
    override fun rememberState(vm: VM): Loadable<State> =
        remember(vm) {
            combine(
                vm.storageService.repository(repositoryURI),
                vm.storageService.repository(from),
                vm.userFlow.stateFlow,
            ) { targetRepository, sourceRepository, userFlowState ->
                State(
                    targetRepository,
                    sourceRepository,
                    userFlowState,
                    vm.userFlow::launch,
                    vm.userFlow::cancel,
                    vm::onClose,
                )
            }
        }.collectAsLoadable()

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        Text(
            if (state.targetRepository.displayName != state.sourceRepository.displayName) {
                stringResource(
                    Res.string.dialog_upload_message_with_repo,
                    state.sourceRepository.storage.displayName,
                    state.targetRepository.displayName,
                    state.targetRepository.storage.displayName,
                )
            } else {
                stringResource(
                    Res.string.dialog_upload_message,
                    state.sourceRepository.storage.displayName,
                    state.targetRepository.storage.displayName,
                )
            },
        )

        RepoToRepoSyncMainContents(
            state.userFlowState,
        )
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        DialogOperationControlButtons(
            state.userFlowState.control(
                onLaunch = state.onLaunch,
                onCancel = state.onCancel,
                onClose = state.onClose,
            ),
        )
    }
}
