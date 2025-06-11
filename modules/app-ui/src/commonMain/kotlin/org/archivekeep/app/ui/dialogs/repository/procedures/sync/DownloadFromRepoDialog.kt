package org.archivekeep.app.ui.dialogs.repository.procedures.sync

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
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
import org.archivekeep.app.ui.domain.wiring.LocalRepoToRepoSyncService
import org.archivekeep.app.ui.domain.wiring.LocalStorageService
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.utils.loading.Loadable

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
    ) : IState {
        override val title: AnnotatedString =
            buildAnnotatedString {
                appendBoldSpan(toRepository.displayName)
                append(" in ")
                appendBoldSpan(toRepository.storage.displayName)
                append(" - copy from")
            }

        override fun dialogWidthModifier(): Modifier = fullWidthDialogWidthModifier
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
        val storageService = LocalStorageService.current
        val syncService = LocalRepoToRepoSyncService.current

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
            remember(state.fromRepository) {
                buildAnnotatedString {
                    append("Copy changes from repository ")
                    appendBoldSpan(state.fromRepository.displayName)
                    append(" in storage ")
                    appendBoldSpan(state.fromRepository.storage.displayName)
                    append(".")
                }
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
