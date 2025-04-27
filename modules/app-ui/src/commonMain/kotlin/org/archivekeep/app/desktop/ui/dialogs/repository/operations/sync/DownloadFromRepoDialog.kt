package org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.sync.RepoToRepoSync
import org.archivekeep.app.core.operations.sync.RepoToRepoSyncService
import org.archivekeep.app.core.persistence.platform.demo.Photos
import org.archivekeep.app.core.persistence.platform.demo.PhotosInHDDB
import org.archivekeep.app.core.persistence.platform.demo.PhotosInLaptopSSD
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.wiring.LocalRepoToRepoSyncService
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.ui.components.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.dialog.fullWidthDialogWidthModifier
import org.archivekeep.app.desktop.ui.dialogs.AbstractDialog
import org.archivekeep.app.desktop.ui.dialogs.repository.operations.sync.parts.RepoToRepoSyncMainContents
import org.archivekeep.app.desktop.ui.utils.appendBoldSpan
import org.archivekeep.app.desktop.utils.collectAsLoadable
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.operations.sync.SyncOperation
import org.archivekeep.utils.loading.Loadable
import org.jetbrains.compose.ui.tooling.preview.Preview

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
    override fun rememberState(vm: DownloadFromRepoDialog.VM): Loadable<State> =
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

@Preview
@Composable
private fun preview1() {
    DialogPreviewColumn {
        DownloadFromRepoDialogPreview1Contents()
    }
}

@Composable
fun DownloadFromRepoDialogPreview1Contents() {
    val dialog =
        DownloadFromRepoDialog(
            PhotosInHDDB.uri,
            PhotosInLaptopSSD.uri,
        )

    val compareResult =
        CompareOperation().calculate(
            Photos.contentsFixture._index,
            Photos
                .withContents {
                    deletePattern("2024/1/.*".toRegex())
                    addStored("2024/4/6-duplicate.JPG", "2024/4/6.JPG")
                }.contentsFixture
                ._index,
        )

    val preparedSync = SyncOperation(RepoToRepoSyncUserFlow.relocationSyncMode).prepareFromComparison(compareResult)

    dialog.renderDialogCard(
        DownloadFromRepoDialog.State(
            PhotosInHDDB.storageRepository,
            PhotosInLaptopSSD.storageRepository,
            RepoToRepoSyncUserFlow.State(
                Loadable.Loaded(
                    value =
                        RepoToRepoSync.State.Prepared(
                            comparisonResult = OptionalLoadable.LoadedAvailable(compareResult),
                            preparedSyncOperation = preparedSync,
                            startExecution = { error("should not be called in preview") },
                        ),
                ),
                mutableStateOf(preparedSync.steps[0].subOperations.toSet()),
            ),
            onLaunch = {},
            onCancel = {},
            onClose = {},
        ),
    )
}
