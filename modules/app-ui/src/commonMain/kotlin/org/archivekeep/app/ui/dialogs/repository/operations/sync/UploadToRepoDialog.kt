package org.archivekeep.app.ui.dialogs.repository.operations.sync

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.sync.RepoToRepoSync
import org.archivekeep.app.core.operations.sync.RepoToRepoSyncService
import org.archivekeep.app.core.persistence.platform.demo.Photos
import org.archivekeep.app.core.persistence.platform.demo.PhotosInHDDA
import org.archivekeep.app.core.persistence.platform.demo.PhotosInLaptopSSD
import org.archivekeep.app.core.persistence.platform.photosAdjustmentA
import org.archivekeep.app.core.persistence.platform.photosAdjustmentB
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.operations.OperationExecutionState
import org.archivekeep.app.ui.components.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.ui.components.designsystem.dialog.fullWidthDialogWidthModifier
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.ui.dialogs.AbstractDialog
import org.archivekeep.app.ui.dialogs.repository.operations.sync.parts.RepoToRepoSyncMainContents
import org.archivekeep.app.ui.domain.wiring.LocalRepoToRepoSyncService
import org.archivekeep.app.ui.domain.wiring.LocalStorageService
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.files.operations.CompareOperation
import org.archivekeep.files.operations.sync.NewFilesSyncStep
import org.archivekeep.files.operations.sync.SyncOperation
import org.archivekeep.testing.fixtures.FixtureRepoBuilder
import org.archivekeep.utils.loading.Loadable
import org.jetbrains.compose.ui.tooling.preview.Preview

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
    ) : IState {
        override val title: AnnotatedString =
            buildAnnotatedString {
                appendBoldSpan(sourceRepository.displayName)
                append(" - copy to")
            }

        override fun dialogWidthModifier(): Modifier = fullWidthDialogWidthModifier
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
            remember(state.targetRepository, state.sourceRepository) {
                buildAnnotatedString {
                    append("Copy changes from ")
                    appendBoldSpan(state.sourceRepository.storage.displayName)
                    append(" to ")

                    if (state.targetRepository.displayName != state.sourceRepository.displayName) {
                        append(" repository ")
                        appendBoldSpan(state.targetRepository.displayName)
                        append(" stored in ")
                    }

                    appendBoldSpan(state.targetRepository.storage.displayName)
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
        UploadToRepoDialogPreview1Contents()
    }
}

@Composable
fun UploadToRepoDialogPreview1Contents() {
    val dialog =
        UploadToRepoDialog(
            PhotosInHDDA.uri,
            PhotosInLaptopSSD.uri,
        )

    val compareResult =
        CompareOperation().calculate(
            Photos.withContents(FixtureRepoBuilder::photosAdjustmentA).contentsFixture._index,
            Photos
                .withContents(FixtureRepoBuilder::photosAdjustmentB)
                .contentsFixture
                ._index,
        )

    val preparedSync = SyncOperation(RepoToRepoSyncUserFlow.relocationSyncMode).prepareFromComparison(compareResult)
    val selectedNewFiles = (preparedSync.steps[1] as NewFilesSyncStep).subOperations.subList(0, 4)

    dialog.renderDialogCard(
        UploadToRepoDialog.State(
            PhotosInHDDA.storageRepository,
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
                mutableStateOf(selectedNewFiles.toSet()),
            ),
            onLaunch = {},
            onCancel = {},
            onClose = {},
        ),
    )
}

@Preview
@Composable
private fun preview2() {
    DialogPreviewColumn {
        UploadToRepoDialogPreview2Contents()
    }
}

@Composable
internal fun UploadToRepoDialogPreview2Contents() {
    val dialog =
        UploadToRepoDialog(
            PhotosInHDDA.uri,
            PhotosInLaptopSSD.uri,
        )

    val compareResult =
        CompareOperation().calculate(
            Photos.withContents(FixtureRepoBuilder::photosAdjustmentA).contentsFixture._index,
            Photos
                .withContents(FixtureRepoBuilder::photosAdjustmentB)
                .contentsFixture
                ._index,
        )

    val preparedSync = SyncOperation(RepoToRepoSyncUserFlow.relocationSyncMode).prepareFromComparison(compareResult)
    val selectedNewFiles = (preparedSync.steps[1] as NewFilesSyncStep).subOperations.subList(0, 4)

    dialog.renderDialogCard(
        UploadToRepoDialog.State(
            PhotosInHDDA.storageRepository,
            PhotosInLaptopSSD.storageRepository,
            RepoToRepoSyncUserFlow.State(
                Loadable.Loaded(
                    value =
                        RepoToRepoSync.JobState(
                            comparisonResult = OptionalLoadable.LoadedAvailable(compareResult),
                            preparedSyncOperation = preparedSync,
                            progressLog = MutableStateFlow("copied: 2024/6/1.JPG\ncopied: 2024/6/2.JPG"),
                            progress =
                                MutableStateFlow(
                                    listOf(
                                        NewFilesSyncStep.Progress(
                                            selectedNewFiles,
                                            selectedNewFiles.subList(0, 2),
                                        ),
                                    ),
                                ),
                            executionState =
                                OperationExecutionState.Finished(
                                    error =
                                        RuntimeException(
                                            "Something went wrong ...",
                                        ),
                                ),
                            inProgressOperationsStats = MutableStateFlow(emptyList())
                        ),
                ),
                mutableStateOf(selectedNewFiles.toSet()),
            ),
            onLaunch = {},
            onCancel = {},
            onClose = {},
        ),
    )
}

@Preview
@Composable
private fun preview3() {
    DialogPreviewColumn {
        UploadToRepoDialogPreview3Contents()
    }
}

@Composable
internal fun UploadToRepoDialogPreview3Contents() {
    val dialog = UploadToRepoDialog(PhotosInHDDA.uri, PhotosInLaptopSSD.uri)

    val compareResult =
        CompareOperation().calculate(
            Photos.withContents(FixtureRepoBuilder::photosAdjustmentA).contentsFixture._index,
            Photos
                .withContents(FixtureRepoBuilder::photosAdjustmentB)
                .contentsFixture
                ._index,
        )

    val preparedSync = SyncOperation(RepoToRepoSyncUserFlow.relocationSyncMode).prepareFromComparison(compareResult)
    val selectedNewFiles = (preparedSync.steps[1] as NewFilesSyncStep).subOperations.subList(0, 4)

    dialog.renderDialogCard(
        UploadToRepoDialog.State(
            PhotosInHDDA.storageRepository,
            PhotosInLaptopSSD.storageRepository,
            RepoToRepoSyncUserFlow.State(
                Loadable.Loaded(
                    value =
                        RepoToRepoSync.JobState(
                            comparisonResult = OptionalLoadable.LoadedAvailable(compareResult),
                            preparedSyncOperation = preparedSync,
                            progressLog = MutableStateFlow("copied: 2024/6/1.JPG\ncopied: 2024/6/2.JPG"),
                            progress =
                                MutableStateFlow(
                                    listOf(
                                        NewFilesSyncStep.Progress(
                                            selectedNewFiles,
                                            selectedNewFiles.subList(0, 2),
                                        ),
                                    ),
                                ),
                            executionState =
                                OperationExecutionState.Finished(
                                    error =
                                        RuntimeException(
                                            "Something went wrong ...",
                                        ),
                                ),
                            inProgressOperationsStats = MutableStateFlow(emptyList())
                        ),
                ),
                mutableStateOf(selectedNewFiles.toSet()),
            ),
            onLaunch = {},
            onCancel = {},
            onClose = {},
        ),
    )
}
