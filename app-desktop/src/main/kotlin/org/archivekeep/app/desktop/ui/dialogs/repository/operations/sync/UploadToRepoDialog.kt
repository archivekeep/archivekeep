package org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.sync.RepoToRepoSyncService
import org.archivekeep.app.core.persistence.platform.demo.Documents
import org.archivekeep.app.core.persistence.platform.demo.hddA
import org.archivekeep.app.core.persistence.platform.demo.hddB
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalRepoToRepoSyncService
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.dialogs.AbstractDialog
import org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync.parts.RepoToRepoSyncFlowButtons
import org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync.parts.RepoToRepoSyncMainContents
import org.archivekeep.app.desktop.utils.asMutableState
import org.archivekeep.app.desktop.utils.collectAsLoadable
import org.archivekeep.files.operations.RelocationSyncMode
import org.archivekeep.utils.loading.Loadable

data class UploadToRepoDialog(
    val repositoryURI: RepositoryURI,
    val from: RepositoryURI,
) : AbstractDialog<UploadToRepoDialog.State, UploadToRepoDialog.VM>() {
    data class State(
        val targetRepository: StorageRepository,
        val sourceRepository: StorageRepository,
        val relocationSyncMode: MutableState<RelocationSyncMode>,
        val userFlowState: RepoToRepoSyncUserFlow.State,
        val onLaunch: () -> Unit,
        val onCancel: () -> Unit,
        val onClose: () -> Unit,
    ) : IState {
        override val title: AnnotatedString =
            buildAnnotatedString {
                append("Push to ${targetRepository.displayName} in ${targetRepository.storage.displayName}")
            }
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
        val storageService = LocalStorageService.current
        val syncService = LocalRepoToRepoSyncService.current

        return remember(storageService, syncService, scope) {
            VM(storageService, syncService, scope, onClose)
        }
    }

    @Composable
    override fun rememberState(vm: UploadToRepoDialog.VM): Loadable<State> {
        val relocationSyncModeMutableState = vm.userFlow.relocationSyncModeFlow.asMutableState()

        return remember(vm) {
            combine(
                vm.storageService.repository(repositoryURI),
                vm.storageService.repository(from),
                vm.userFlow.stateFlow,
            ) { targetRepository, sourceRepository, userFlowState ->
                State(
                    targetRepository,
                    sourceRepository,
                    relocationSyncModeMutableState,
                    userFlowState,
                    vm.userFlow::launch,
                    vm.userFlow::cancel,
                    vm::onClose,
                )
            }
        }.collectAsLoadable()
    }

    @Composable
    override fun ColumnScope.renderContent(state: State) {
        Text(
            remember(state.sourceRepository) {
                buildAnnotatedString {
                    append(
                        "From to ${state.sourceRepository.displayName} in ${state.sourceRepository.storage.displayName}.",
                    )
                }
            },
        )

        RepoToRepoSyncMainContents(
            state.relocationSyncMode,
            state.userFlowState,
        )
    }

    @Composable
    override fun RowScope.renderButtons(state: State) {
        RepoToRepoSyncFlowButtons(
            userFlowState = state.userFlowState,
            onLaunch = state.onLaunch,
            onCancel = state.onCancel,
            onClose = state.onClose,
        )
    }
}

@Preview
@Composable
private fun preview1() {
    val DocumentsInHDDA = Documents.inStorage(hddA.reference).storageRepository
    val DocumentsInHDDB = Documents.inStorage(hddB.reference).storageRepository

    DialogPreviewColumn {
        val dialog =
            UploadToRepoDialog(
                DocumentsInHDDA.uri,
                DocumentsInHDDB.uri,
            )

        dialog.renderDialogCard(
            UploadToRepoDialog.State(
                DocumentsInHDDA,
                DocumentsInHDDB,
                mutableStateOf(RepoToRepoSyncUserFlow.defaultRelocationSyncMode),
                RepoToRepoSyncUserFlow.State(
                    Loadable.Loading,
                ),
                onLaunch = {},
                onCancel = {},
                onClose = {},
            ),
        )
    }
}
