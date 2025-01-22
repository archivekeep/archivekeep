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

data class UploadToRepoDialog(
    val repositoryURI: RepositoryURI,
    val from: RepositoryURI,
) : Dialog {
    data class State(
        val targetRepository: StorageRepository,
        val sourceRepository: StorageRepository,
        val userFlow: RepoToRepoSyncUserFlow.State,
    )

    inner class VM(
        val storageService: StorageService,
        syncService: SyncService,
        val scope: CoroutineScope,
    ) {
        val sync = syncService.getRepoToRepoSync(from, repositoryURI)
        val userFlow = RepoToRepoSyncUserFlow(scope, sync)

        val state =
            combine(
                storageService.repository(repositoryURI),
                storageService.repository(from),
                userFlow.stateFlow,
            ) { targetRepository, sourceRepository, userFlowState ->
                State(
                    targetRepository,
                    sourceRepository,
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
                    remember(state.targetRepository) {
                        buildAnnotatedString {
                            append("Push to ${state.targetRepository.displayName} in ${state.targetRepository.storage.displayName}")
                        }
                    },
                content = {
                    Text(
                        remember(state.sourceRepository) {
                            buildAnnotatedString {
                                append(
                                    "From to ${state.sourceRepository.displayName} in ${state.sourceRepository.storage.displayName}.",
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
