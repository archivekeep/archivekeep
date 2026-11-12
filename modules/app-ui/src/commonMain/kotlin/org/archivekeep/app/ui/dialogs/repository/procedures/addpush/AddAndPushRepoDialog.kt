package org.archivekeep.app.ui.dialogs.repository.procedures.addpush

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.JobState
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.NotReadyAddPushProcess
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.PreparingAddPushProcess
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.ReadyAddPushProcess
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.base.layout.ScrollableColumn
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumnWithGuessedWidth
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.dialog.fullWidthDialogWidthModifier
import org.archivekeep.app.ui.components.feature.LoadableGuard
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlButtons
import org.archivekeep.app.ui.components.feature.dialogs.operations.ExecutionErrorIfPresent
import org.archivekeep.app.ui.components.feature.manyselect.DestinationManySelect
import org.archivekeep.app.ui.components.feature.manyselect.rememberManySelectForRender
import org.archivekeep.app.ui.components.feature.operations.InProgressOperationsList
import org.archivekeep.app.ui.components.feature.operations.IndexUpdatePreparationProgress
import org.archivekeep.app.ui.components.feature.operations.LocalIndexUpdateProgress
import org.archivekeep.app.ui.components.feature.operations.SyncProgress
import org.archivekeep.app.ui.dialogs.repository.AbstractRepositoryDialog
import org.archivekeep.app.ui.dialogs.repository.procedures.addpush.AddAndPushRepoDialogViewModel.VMState
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.asMutableState
import org.archivekeep.app.ui.utils.collectAsLoadable
import org.archivekeep.app.ui.utils.contextualStorageReference
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_loading
import org.archivekeep.ui.resources.common_preparing
import org.archivekeep.ui.resources.dialog_add_and_push_title
import org.archivekeep.ui.resources.dialog_push_title
import org.archivekeep.ui.resources.manyselect_files_to_add_and_push
import org.archivekeep.ui.resources.manyselect_files_to_add_and_push_all
import org.archivekeep.ui.resources.manyselect_moves
import org.archivekeep.ui.resources.manyselect_moves_all
import org.archivekeep.ui.resources.sync_push_to
import org.archivekeep.utils.collections.ifNotEmpty
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.jetbrains.compose.resources.stringResource

class AddAndPushRepoDialog(
    uri: RepositoryURI,
) : AbstractRepositoryDialog<VMState, AddAndPushRepoDialogViewModel>(uri) {
    override fun dialogWidthModifier(state: VMState) = fullWidthDialogWidthModifier

    @Composable
    override fun title(state: VMState) =
        buildAnnotatedString {
            appendBoldSpan(state.repoName)
            append(" - ")
            append(stringResource(Res.string.dialog_add_and_push_title))
        }

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
                                .filter { it.repositoryState.connectionState.isAccessible }
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
            is NotReadyAddPushProcess -> {
                Text(stringResource(Res.string.common_loading))
            }

            is PreparingAddPushProcess -> {
                LabelText(stringResource(Res.string.common_preparing))
                IndexUpdatePreparationProgress(status.addPreparationProgress)
            }

            is ReadyAddPushProcess -> {
                val movesManySelect =
                    rememberManySelectForRender(
                        status.addPreprationResult.moves,
                        state.selectedMoves,
                        stringResource(Res.string.manyselect_moves),
                        allItemsLabel = { stringResource(Res.string.manyselect_moves_all, it.toString()) },
                        itemLabelText = { "${it.from} -> ${it.to}" },
                    )
                val filesManySelect =
                    rememberManySelectForRender(
                        status.addPreprationResult.newFiles,
                        state.selectedFilenames,
                        stringResource(Res.string.manyselect_files_to_add_and_push),
                        allItemsLabel = { stringResource(Res.string.manyselect_files_to_add_and_push_all, it.toString()) },
                        itemLabelText = { it.fileName },
                    )

                val guessedWidth = max(filesManySelect.guessedWidth, movesManySelect.guessedWidth)

                ScrollableLazyColumnWithGuessedWidth(guessedWidth) {
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

                    status.addPreprationResult.newFileNames.ifNotEmpty {
                        filesManySelect.render(this)
                    }
                }
            }

            is JobState -> {
                ScrollableColumn(Modifier.weight(1f, fill = false)) {
                    Spacer(Modifier.height(4.dp))

                    LocalIndexUpdateProgress(status.jobState.moveProgress, status.jobState.addProgress)

                    Spacer(Modifier.height(4.dp))

                    LabelText(stringResource(Res.string.dialog_push_title))

                    Spacer(Modifier.height(12.dp))

                    status.jobState.pushProgress.forEach { (task, progress) ->
                        val repo = task.destinationRepoID

                        val storageRepository =
                            state.otherRepositoryCandidates.mapIfLoadedOrDefault(null) {
                                it.firstOrNull { it.uri == repo }
                            }

                        Text(
                            stringResource(
                                Res.string.sync_push_to,
                                if (storageRepository != null) {
                                    contextualStorageReference(state.repoName, storageRepository)
                                } else {
                                    repo.toString()
                                },
                            ),
                        )
                        SyncProgress(progress.subTasks)

                        ExecutionErrorIfPresent(status.jobState.executionState)
                    }

                    InProgressOperationsList(status.jobState.inProgressOperationsProgress)
                }
            }
        }
    }

    @Composable
    override fun RowScope.renderButtons(state: VMState) {
        DialogOperationControlButtons(state.controlState)
    }
}
