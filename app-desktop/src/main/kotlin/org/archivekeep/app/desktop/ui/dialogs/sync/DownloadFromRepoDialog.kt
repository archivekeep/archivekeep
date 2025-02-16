package org.archivekeep.app.desktop.ui.dialogs.sync

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.derived.SyncService
import org.archivekeep.app.core.utils.generics.mapToLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.domain.wiring.LocalSyncService
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogCardWithDialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayWithLoadableGuard
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.ui.dialogs.sync.parts.RepoToRepoSyncFlowButtons
import org.archivekeep.app.desktop.ui.dialogs.sync.parts.RepoToRepoSyncMainContents
import org.archivekeep.utils.Loadable

data class DownloadFromRepoDialog(
    val from: RepositoryURI,
    val to: RepositoryURI,
) : Dialog {
    data class State(
        val fromRepository: StorageRepository,
        val toRepository: StorageRepository,
        val userFlow: RepoToRepoSyncUserFlow.State,
    )

    inner class VM(
        val storageService: StorageService,
        syncService: SyncService,
        val scope: CoroutineScope,
    ) {
        val sync = syncService.getRepoToRepoSync(from, to)
        val userFlow = RepoToRepoSyncUserFlow(scope, sync)

        val state =
            combine(
                storageService.repository(from),
                storageService.repository(to),
                userFlow.stateFlow,
            ) { fromRepository, toRepository, userFlowState ->
                State(
                    fromRepository,
                    toRepository,
                    userFlowState,
                )
            }.mapToLoadable().stateIn(scope, SharingStarted.WhileSubscribed(), Loadable.Loading)
    }

    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val storageService = LocalStorageService.current
        val syncService = LocalSyncService.current
        val scope = rememberCoroutineScope()

        val vm =
            remember(storageService, syncService, scope) {
                VM(storageService, syncService, scope)
            }

        DialogOverlayWithLoadableGuard(vm.state.collectAsState().value, onDismissRequest = onClose) { state ->
            DialogCardWithDialogInnerContainer(
                title =
                    remember(state.fromRepository) {
                        buildAnnotatedString {
                            append(
                                "Download from ${state.fromRepository.displayName} in ${state.fromRepository.storage.displayName}",
                            )
                        }
                    },
                content = {
                    Text(
                        remember(state.toRepository) {
                            buildAnnotatedString {
                                append(
                                    "To ${state.toRepository.displayName} in ${state.toRepository.storage.displayName}.",
                                )
                            }
                        },
                    )

                    RepoToRepoSyncMainContents(state.userFlow)
                },
                bottomContent = {
                    RepoToRepoSyncFlowButtons(
                        userFlowState = state.userFlow,
                        onLaunch = vm.userFlow::launch,
                        onCancel = vm.userFlow::cancel,
                        onClose = onClose,
                    )
                },
            )
        }
    }
}
