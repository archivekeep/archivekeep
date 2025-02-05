package org.archivekeep.app.desktop.ui.dialogs.addpush

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.LaunchedAddPushProcess
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.NotReadyAddPushProcess
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.ReadyAddPushProcess
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.ui.components.DestinationManySelect
import org.archivekeep.app.desktop.ui.components.FileManySelect
import org.archivekeep.app.desktop.ui.components.ItemManySelect
import org.archivekeep.app.desktop.ui.components.LoadableGuard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogInnerContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogOverlayCard
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogSecondaryButton
import org.archivekeep.app.desktop.ui.dialogs.Dialog
import org.archivekeep.app.desktop.utils.asMutableState
import org.archivekeep.app.desktop.utils.collectAsLoadable

class AddAndPushRepoDialog(
    val repositoryURI: RepositoryURI,
) : Dialog {
    @Composable
    override fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        val vm = rememberAddAndPushDialogViewModel(scope, repositoryURI, onClose)

        AddAndPushDialogContents(vm, onClose)
    }
}

@Composable
private fun AddAndPushDialogContents(
    vm: AddAndPushDialogViewModel,
    onClose: () -> Unit,
) {
    DialogOverlayCard(onDismissRequest = onClose) {
        LoadableGuard(
            vm.repoName.collectAsLoadable(),
            vm.currentVMState.collectAsLoadable(),
        ) { repoName, state ->

            DialogInnerContainer(
                rememberDialogTitle(repoName),
                content = {
                    when (val status = state.state) {
                        is NotReadyAddPushProcess ->
                            Text("Loading")

                        is ReadyAddPushProcess -> {
                            LoadableGuard(
                                state.otherRepositoryCandidates,
                            ) {
                                DestinationManySelect(
                                    it,
                                    vm.selectedDestinationRepositories.asMutableState(),
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            if (state.allMoves.isNotEmpty()) {
                                ItemManySelect(
                                    "Moves",
                                    allItemsLabel = { "All moves ($it)" },
                                    itemLabel = { "${it.from} -> ${it.to}" },
                                    allItems = state.allMoves,
                                    vm.selectedMoves.asMutableState(),
                                )
                                Spacer(Modifier.height(12.dp))
                            }

                            if (state.allNewFiles.isNotEmpty()) {
                                FileManySelect("Files to add and push:", state.allNewFiles, vm.selectedFilenames.asMutableState())
                            }
                        }

                        is LaunchedAddPushProcess -> {
                            val addProgress =
                                buildAnnotatedString {
                                    if (status.options.movesToExecute.isNotEmpty()) {
                                        appendLine("Added ${status.moveProgress.moved.size} / ${status.options.movesToExecute.size}")
                                    }
                                    if (status.options.filesToAdd.isNotEmpty()) {
                                        appendLine("Moved ${status.addProgress.added.size} / ${status.options.filesToAdd.size}")
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
                },
                bottomContent = {
                    DialogButtonContainer {
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
                                onClick = vm::cancel,
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        DialogDismissButton(
                            if (state.canHide) {
                                "Hide"
                            } else {
                                "Dismiss"
                            },
                            onClick = vm.onClose,
                        )
                    }
                },
            )
        }
    }
}

// TODO: preview after refactor sort-out
// @Preview
// @Composable
// private fun AddAndPushDialogContentsCompletedPreview() {
//    val DocumentsInHDDA = Documents.inStorage(hddA.reference).storageRepository
//    val DocumentsInHDDB = Documents.inStorage(hddB.reference).storageRepository
//
//    val selectedFilenames = setOf("2024/08/01.JPG", "2024/08/02.JPG")
//    val selectedDestinations = setOf(DocumentsInHDDA.uri, DocumentsInHDDB.uri)
//
//    val vm =
//        AddAndPushDialogViewModel(
//            repoName = mutableStateOf(Loadable.Loaded("Documents")),
//            otherRepositoryCandidates =
//                mutableStateOf(
//                    Loadable.Loaded(
//                        listOf(
//                            DocumentsInHDDA,
//                            DocumentsInHDDB,
//                        ),
//                    ),
//                ),
//            addPushStatus =
//                mutableStateOf(
//                    LaunchedAddPushProcess(
//                        IndexStatus(emptyList(), emptyList()),
//                        LaunchOptions(selectedFilenames, selectedDestinations),
//                        addProgress = AddProgress(selectedFilenames, emptyMap(), true),
//                        pushProgress =
//                            mapOf(
//                                DocumentsInHDDA.uri to PushProgress(selectedFilenames, emptyMap(), true),
//                                DocumentsInHDDB.uri to PushProgress(selectedFilenames, emptyMap(), true),
//                            ),
//                        finished = true,
//                    ),
//                ),
//            allNewFiles = mutableStateOf(selectedFilenames.toList()),
//            onClose = {},
//        )
//
//    AddAndPushDialogContents(vm, {})
// }

@Composable
private fun rememberDialogTitle(displayName: String) =
    remember(displayName) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(displayName)
                append(": ")
            }
            append("add and push")
        }
    }
