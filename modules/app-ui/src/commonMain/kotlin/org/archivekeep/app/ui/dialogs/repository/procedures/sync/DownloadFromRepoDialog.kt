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
import org.archivekeep.ui.resources.common_in
import org.archivekeep.ui.resources.dialog_copy_from_title
import org.archivekeep.ui.resources.dialog_download_message
import org.archivekeep.utils.loading.Loadable
import org.jetbrains.compose.resources.stringResource

data class DownloadFromRepoDialog(
    val from: RepositoryURI,
    val to: RepositoryURI,
) : AbstractDialog<DownloadFromRepoDialog.State, DownloadFromRepoDialog.VM>() {
    data class State(
        val fromRepository: StorageRepository,
        val toRepository: StorageRepository,
        val userFlowState: RepoToRepoSyncUserFlow.State,
        val onLaunch: () -> Unit,
        val onCancel: () -> Unit,
        val onClose: () -> Unit,
    )

    override fun dialogWidthModifier(state: State) = fullWidthDialogWidthModifier

    @Composable
    override fun title(state: State) =
        buildAnnotatedString {
            appendBoldSpan(state.toRepository.displayName)
            append(" ")
            append(stringResource(Res.string.common_in))
            append(" ")
            appendBoldSpan(state.toRepository.storage.displayName)
            append(" - ")
            append(stringResource(Res.string.dialog_copy_from_title))
        }

    inner class VM(
        val storageService: StorageService,
        repoToRepoSyncService: RepoToRepoSyncService,
        val scope: CoroutineScope,
        val _onClose: () -> Unit,
    ) : IVM {
        val sync = repoToRepoSyncService.getRepoToRepoSync(from, to)
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
                vm.storageService.repository(from),
                vm.storageService.repository(to),
                vm.userFlow.stateFlow,
            ) { fromRepository, toRepository, userFlowState ->
                State(
                    fromRepository,
                    toRepository,
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
            stringResource(
                Res.string.dialog_download_message,
                state.fromRepository.displayName,
                state.fromRepository.storage.displayName,
            ),
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
