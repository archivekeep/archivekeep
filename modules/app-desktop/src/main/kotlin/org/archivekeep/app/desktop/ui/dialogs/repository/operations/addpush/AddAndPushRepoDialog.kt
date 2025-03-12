package org.archivekeep.app.desktop.ui.dialogs.repository.operations.addpush

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.LaunchedAddPushProcess
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.NotReadyAddPushProcess
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.ReadyAddPushProcess
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInBackBlaze
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInHDDA
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInLaptopSSD
import org.archivekeep.app.core.persistence.platform.demo.DocumentsInSSDKeyChain
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.ui.components.DestinationManySelect
import org.archivekeep.app.desktop.ui.components.FileManySelect
import org.archivekeep.app.desktop.ui.components.ItemManySelect
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPreviewColumn
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogSecondaryButton
import org.archivekeep.app.desktop.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.desktop.ui.dialogs.repository.operations.addpush.AddAndPushRepoDialogViewModel.VMState
import org.archivekeep.app.desktop.utils.asMutableState
import org.archivekeep.app.desktop.utils.collectAsLoadable
import org.archivekeep.files.operations.AddOperation
import org.archivekeep.utils.collections.ifNotEmpty
import org.archivekeep.utils.loading.Loadable

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

            is ReadyAddPushProcess -> {
                LoadableGuard(
                    state.otherRepositoryCandidates,
                ) {
                    DestinationManySelect(
                        it,
                        state.selectedDestinationRepositories,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                status.addPreprationResult.moves.ifNotEmpty { moves ->
                    ItemManySelect(
                        "Moves",
                        allItemsLabel = { "All moves ($it)" },
                        itemLabelText = { "${it.from} -> ${it.to}" },
                        allItems = moves,
                        state.selectedMoves,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                status.addPreprationResult.newFiles.ifNotEmpty { newFiles ->
                    FileManySelect(
                        "Files to add and push:",
                        newFiles,
                        state.selectedFilenames,
                    )
                }
            }

            is LaunchedAddPushProcess -> {
                val addProgress =
                    buildAnnotatedString {
                        if (status.options.movesToExecute.isNotEmpty()) {
                            appendLine("Moved ${status.moveProgress.moved.size} / ${status.options.movesToExecute.size}")
                        }
                        if (status.options.filesToAdd.isNotEmpty()) {
                            appendLine("Added ${status.addProgress.added.size} / ${status.options.filesToAdd.size}")
                        }

                        status.pushProgress.forEach { (repo, pp) ->
                            appendLine(
                                "Pushed ${pp.moved.size} / ${status.options.movesToExecute.size} moves, " +
                                    "${pp.added.size} / ${status.options.filesToAdd.size} files to $repo",
                            )
                        }
                    }

                Text(addProgress)
            }
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: VMState) {
        if (state.showLaunch) {
            DialogPrimaryButton(
                "Launch",
                onClick = state::launch,
                enabled = state.canLaunch,
            )
        }

        if (state.canStop) {
            DialogSecondaryButton(
                (if (false) "Stopping ..." else "Stop"),
                onClick = state.onCancel,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        DialogDismissButton(
            if (state.canHide) {
                "Hide"
            } else {
                "Dismiss"
            },
            onClick = state.onClose,
        )
    }
}

@Preview
@Composable
private fun AddAndPushDialogContentsCompletedPreview() {
    val allMoves =
        listOf(
            AddOperation.PreparationResult.Move("Invoices/2024/01.PDF", "Contracting/Invoices/2024/01.PDF"),
            AddOperation.PreparationResult.Move("Invoices/2024/02.PDF", "Contracting/Invoices/2024/02.PDF"),
        )
    val allNewFiles = listOf("2024/08/01.JPG", "2024/08/02.JPG")
    val allDestinations = setOf(DocumentsInBackBlaze.uri, DocumentsInSSDKeyChain.uri)

    DialogPreviewColumn {
        val dialog = AddAndPushRepoDialog(DocumentsInLaptopSSD.uri)

        dialog.renderDialogCard(
            VMState(
                repoName = "Documents",
                ReadyAddPushProcess(
                    AddOperation.PreparationResult(
                        newFiles = allNewFiles,
                        moves = allMoves,
                        missingFiles = emptyList(),
                    ),
                    launch = {},
                ),
                selectedFilenames = mutableStateOf(allNewFiles.toSet()),
                selectedMoves = mutableStateOf(allMoves.toSet()),
                selectedDestinationRepositories = mutableStateOf(allDestinations),
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
}
