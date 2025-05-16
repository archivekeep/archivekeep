package org.archivekeep.app.ui.dialogs.repository.procedures.addpush

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInBackBlaze
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInHDDA
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInLaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInSSDKeyChain
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.JobState
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.NotReadyAddPushProcess
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.PreparingAddPushProcess
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.ReadyAddPushProcess
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.base.layout.IntrinsicSizeWrapperLayout
import org.archivekeep.app.ui.components.base.layout.ScrollableColumn
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumn
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.dialog.previewWith
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRow
import org.archivekeep.app.ui.components.designsystem.progress.ProgressRowList
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.ExecutionErrorIfPresent
import org.archivekeep.app.ui.components.feature.manyselect.DestinationManySelect
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectForRender
import org.archivekeep.app.ui.components.feature.operations.InProgressOperationsList
import org.archivekeep.app.ui.components.feature.operations.IndexUpdatePreparationProgress
import org.archivekeep.app.ui.components.feature.operations.LocalIndexUpdateProgress
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.procedures.addpush.AddAndPushRepoDialogViewModel.VMState
import org.archivekeep.app.ui.utils.asMutableState
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.app.ui.utils.contextualStorageReference
import org.archivekeep.files.procedures.indexupdate.IndexUpdateAddProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateMoveProgress
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.utils.procedures.ProcedureExecutionState
import org.archivekeep.utils.collections.ifNotEmpty
import org.archivekeep.utils.filesAutoPlural
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.jetbrains.compose.ui.tooling.preview.Preview

class AddAndPushRepoDialog(
    uri: RepositoryURI,
) : AbstractRepositoryDialog<VMState, AddAndPushRepoDialogViewModel>(uri) {
    @Composable
    override fun rememberVM(
        scope: CoroutineScope,
        repository: Repository,
        onClose: () -> Unit,
    ): AddAndPushRepoDialogViewModel = rememberAddAndPushDialogViewModel(scope, uri, onClose)

    @Composable
    override fun rememberState(vm: AddAndPushRepoDialogViewModel): Loadable<VMState> {
        val selectedDestinationRepositories = vm.selectedDestinationRepositories.asMutableState()
        val selectedFilenames = vm.selectedFilenames.asMutableState()
        val selectedMoves = vm.selectedMoves.asMutableState()

        return remember(vm) {
            combine(
                vm.repoName,
                vm.currentStatusFlow,
                vm.otherRepositoryCandidates.onEach { v ->
                    if (v is Loadable.Loaded) {
                        selectedDestinationRepositories.value =
                            v.value
                                .filter { it.repositoryState.connectionState.isConnected }
                                .map { it.uri }
                                .toSet()
                    }
                },
            ) { repoName, currentStatus, otherRepositoryCandidates ->
                VMState(
                    repoName,
                    currentStatus,
                    selectedDestinationRepositories,
                    selectedFilenames,
                    selectedMoves,
                    otherRepositoryCandidates,
                    onCancel = vm::cancel,
                    onClose = vm::onClose,
                )
            }
        }.collectAsLoadable()
    }

    @Composable
    override fun ColumnScope.renderContent(state: VMState) {
        when (val status = state.state) {
            is NotReadyAddPushProcess ->
                Text("Loading")

            is PreparingAddPushProcess -> {
                LabelText("Preparing add and push operation:")
                IndexUpdatePreparationProgress(status.addPreparationProgress)
            }

            is ReadyAddPushProcess -> {
                val movesManySelect =
                    rememberManySelectForRender(
                        status.addPreprationResult.moves,
                        state.selectedMoves,
                        "Moves",
                        allItemsLabel = { "All moves ($it)" },
                        itemLabelText = { "${it.from} -> ${it.to}" },
                    )
                val filesManySelect =
                    rememberManySelectForRender(
                        status.addPreprationResult.newFiles,
                        state.selectedFilenames,
                        "Files to add and push",
                        allItemsLabel = { "All new files ($it)" },
                        itemLabelText = { it },
                    )

                val guessedWidth = max(filesManySelect.guessedWidth, movesManySelect.guessedWidth)

                IntrinsicSizeWrapperLayout(
                    minIntrinsicWidth = guessedWidth,
                    maxIntrinsicWidth = guessedWidth,
                ) {
                    ScrollableLazyColumn {
                        item {
                            LoadableGuard(
                                state.otherRepositoryCandidates,
                            ) {
                                DestinationManySelect(
                                    it,
                                    state.selectedDestinationRepositories,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        status.addPreprationResult.moves.ifNotEmpty {
                            movesManySelect.render(this)
                            item {
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        status.addPreprationResult.newFiles.ifNotEmpty {
                            filesManySelect.render(this)
                        }
                    }
                }
            }

            is JobState -> {
                ScrollableColumn(Modifier.weight(1f, fill = false)) {
                    Spacer(Modifier.height(4.dp))

                    val mte = status.options.movesToExecute
                    val fta = status.options.filesToAdd

                    LocalIndexUpdateProgress(mte, fta, status.moveProgress, status.addProgress)

                    Spacer(Modifier.height(4.dp))

                    LabelText("Push progress")

                    ProgressRowList {
                        status.pushProgress.forEach { (repo, pp) ->
                            val storageRepository =
                                state.otherRepositoryCandidates.mapIfLoadedOrDefault(null) {
                                    it.firstOrNull { it.uri == repo }
                                }

                            ProgressRow(
                                progress = { (pp.moved.size + pp.added.size) / (mte.size.toFloat() + fta.size.toFloat()) },
                                text =
                                    if (storageRepository != null) {
                                        "Push to ${contextualStorageReference(state.repoName, storageRepository)}"
                                    } else {
                                        "Push to $repo"
                                    },
                            ) {
                                if (mte.isNotEmpty()) {
                                    ProgressRow(
                                        progress = { pp.moved.size / mte.size.toFloat() },
                                        "Moved ${pp.moved.size} of ${mte.size} ${filesAutoPlural(mte)}",
                                    )
                                }
                                if (fta.isNotEmpty()) {
                                    ProgressRow(
                                        progress = { pp.added.size / fta.size.toFloat() },
                                        "Added ${pp.added.size} of ${fta.size} ${filesAutoPlural(fta)}",
                                    )
                                }
                            }

                            ExecutionErrorIfPresent(status.executionState)
                        }
                    }

                    InProgressOperationsList(status.inProgressOperationsProgress)
                }
            }
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: VMState) {
        DialogOperationControlButtons(state.controlState)
    }
}

val allTestMoves =
    listOf(
        IndexUpdateProcedure.PreparationResult.Move("Invoices/2024/01.PDF", "Contracting/Invoices/2024/01.PDF"),
        IndexUpdateProcedure.PreparationResult.Move("Invoices/2024/02.PDF", "Contracting/Invoices/2024/02.PDF"),
        IndexUpdateProcedure.PreparationResult.Move("Invoices/2024/03.PDF", "Contracting/Invoices/2024/03.PDF"),
        IndexUpdateProcedure.PreparationResult.Move("Invoices/2024/04.PDF", "Contracting/Invoices/2024/04.PDF"),
    )

val allNewFiles =
    listOf(
        "2024/08/01.JPG",
        "2024/08/02.JPG",
        "2024/08/03.JPG",
        "2024/08/04.JPG",
        "2024/08/05.JPG",
    )

val selectedDestinations = setOf(DocumentsInBackBlaze.uri, DocumentsInSSDKeyChain.uri)

@Preview
@Composable
private fun AddAndPushDialogContentsCompletedPreview() {
    AddAndPushRepoDialog(DocumentsInLaptopSSD.uri)
        .previewWith(
            VMState(
                repoName = "Documents",
                ReadyAddPushProcess(
                    IndexUpdateProcedure.PreparationResult(
                        newFiles = allNewFiles,
                        moves = allTestMoves,
                        missingFiles = emptyList(),
                        errorFiles = emptyMap(),
                    ),
                    launch = {},
                ),
                selectedFilenames = mutableStateOf(allNewFiles.toSet()),
                selectedMoves = mutableStateOf(allTestMoves.toSet()),
                selectedDestinationRepositories = mutableStateOf(selectedDestinations),
                otherRepositoryCandidates =
                    Loadable.Loaded(
                        listOf(
                            DocumentsInBackBlaze.storageRepository,
                            DocumentsInSSDKeyChain.storageRepository,
                            DocumentsInHDDA.storageRepository,
                        ),
                    ),
                onCancel = {},
                onClose = {},
            ),
        )
}

@Preview
@Composable
private fun AddAndPushDialogContentsCompletedPreview2() {
    AddAndPushRepoDialog(DocumentsInLaptopSSD.uri)
        .previewWith(
            VMState(
                repoName = "Documents",
                JobState(
                    options = AddAndPushProcedure.LaunchOptions(allNewFiles.toSet(), emptySet(), selectedDestinations.toSet()),
                    addProgress = IndexUpdateAddProgress(emptySet(), emptyMap(), false),
                    moveProgress = IndexUpdateMoveProgress(emptySet(), emptyMap(), false),
                    pushProgress = selectedDestinations.associateWith { AddAndPushProcedure.PushProgress(emptySet(), emptySet(), emptyMap(), false) },
                    executionState = ProcedureExecutionState.Running,
                    inProgressOperationsProgress = emptyList()
                ),
                selectedFilenames = mutableStateOf(allNewFiles.toSet()),
                selectedMoves = mutableStateOf(emptySet()),
                selectedDestinationRepositories = mutableStateOf(selectedDestinations),
                otherRepositoryCandidates =
                    Loadable.Loaded(
                        listOf(
                            DocumentsInBackBlaze.storageRepository,
                            DocumentsInSSDKeyChain.storageRepository,
                            DocumentsInHDDA.storageRepository,
                        ),
                    ),
                onCancel = {},
                onClose = {},
            ),
        )
}

@Preview
@Composable
private fun AddAndPushDialogContentsCompletedPreview3() {
    AddAndPushRepoDialog(DocumentsInLaptopSSD.uri)
        .previewWith(
            VMState(
                repoName = "Documents",
                JobState(
                    options = AddAndPushProcedure.LaunchOptions(allNewFiles.toSet(), allTestMoves.toSet(), selectedDestinations.toSet()),
                    addProgress = IndexUpdateAddProgress(setOf(allNewFiles[0]), emptyMap(), false),
                    moveProgress = IndexUpdateMoveProgress(allTestMoves.subList(0, 3).toSet(), emptyMap(), false),
                    pushProgress = selectedDestinations.associateWith { AddAndPushProcedure.PushProgress(emptySet(), emptySet(), emptyMap(), false) },
                    executionState = ProcedureExecutionState.Running,
                    inProgressOperationsProgress = emptyList()
                ),
                selectedFilenames = mutableStateOf(allNewFiles.toSet()),
                selectedMoves = mutableStateOf(allTestMoves.toSet()),
                selectedDestinationRepositories = mutableStateOf(selectedDestinations),
                otherRepositoryCandidates =
                    Loadable.Loaded(
                        listOf(
                            DocumentsInBackBlaze.storageRepository,
                            DocumentsInSSDKeyChain.storageRepository,
                            DocumentsInHDDA.storageRepository,
                        ),
                    ),
                onCancel = {},
                onClose = {},
            ),
        )
}
