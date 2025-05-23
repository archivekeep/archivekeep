package org.archivekeep.app.ui.dialogs.repository.procedures.addpush

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.JobState
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.LaunchOptions
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedure.ReadyAddPushProcess
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.components.designsystem.dialog.fullWidthDialogWidthModifier
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState
import org.archivekeep.app.ui.components.feature.dialogs.operations.toDialogOperationControlState
import org.archivekeep.app.ui.dialogs.AbstractDialog
import org.archivekeep.app.ui.domain.data.getSyncCandidates
import org.archivekeep.app.ui.domain.wiring.LocalAddPushService
import org.archivekeep.app.ui.domain.wiring.LocalRepoService
import org.archivekeep.app.ui.domain.wiring.LocalStorageService
import org.archivekeep.app.ui.utils.appendBoldSpan
import org.archivekeep.app.ui.utils.stickToFirstNotNullAsState
import org.archivekeep.files.procedures.indexupdate.IndexUpdateProcedure
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.loading.mapToLoadable

class AddAndPushRepoDialogViewModel(
    val scope: CoroutineScope,
    val storageService: StorageService,
    val repositoryService: RepositoryService,
    val repositoryURI: RepositoryURI,
    val addAndPushProcedure: AddAndPushProcedure,
    val _onClose: () -> Unit,
) : AbstractDialog.IVM {
    val selectedDestinationRepositories: MutableStateFlow<Set<RepositoryURI>> = MutableStateFlow(emptySet())
    val selectedFilenames: MutableStateFlow<Set<IndexUpdateProcedure.PreparationResult.NewFile>> = MutableStateFlow(emptySet())
    val selectedMoves: MutableStateFlow<Set<IndexUpdateProcedure.PreparationResult.Move>> = MutableStateFlow(emptySet())

    val repoName = repositoryService.getRepository(repositoryURI).informationFlow.map { it.displayName }

    val currentOperation = addAndPushProcedure.currentJobFlow.stickToFirstNotNullAsState(scope)

    val otherRepositoryCandidates = getSyncCandidates(storageService, repositoryURI).mapToLoadable()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentStatusFlow =
        currentOperation.flatMapLatest {
            it?.state
                ?: addAndPushProcedure.prepare().onStart { emit(AddAndPushProcedure.NotReadyAddPushProcess) }
        }

    data class VMState(
        val repoName: String,
        val state: AddAndPushProcedure.State,
        val selectedDestinationRepositories: MutableState<Set<RepositoryURI>>,
        val selectedFilenames: MutableState<Set<IndexUpdateProcedure.PreparationResult.NewFile>>,
        val selectedMoves: MutableState<Set<IndexUpdateProcedure.PreparationResult.Move>>,
        val otherRepositoryCandidates: Loadable<List<StorageRepository>>,
        val onCancel: () -> Unit,
        val onClose: () -> Unit,
    ) : AbstractDialog.IState {
        override val title =
            buildAnnotatedString {
                appendBoldSpan(repoName)
                append(" - ")
                append("add and push")
            }

        val controlState: DialogOperationControlState by derivedStateOf {
            when (state) {
                is JobState ->
                    state.jobState.executionState.toDialogOperationControlState(
                        onCancel = onCancel,
                        onHide = onClose,
                        onClose = onClose,
                    )
                AddAndPushProcedure.NotReadyAddPushProcess, is AddAndPushProcedure.PreparingAddPushProcess ->
                    DialogOperationControlState.NotRunning(onLaunch = {}, onClose = onClose, canLaunch = false)
                is ReadyAddPushProcess -> {
                    val candidates = otherRepositoryCandidates.mapIfLoadedOrDefault(emptyList()) { it }
                    val selections = selectedDestinationRepositories

                    val anyOperationSelected = selectedFilenames.value.isNotEmpty() || selectedMoves.value.isNotEmpty()

                    val candidateSelectedAndValid =
                        selections.value.isNotEmpty() &&
                            selections.value.all { selection ->
                                candidates
                                    .first { it.uri == selection }
                                    .repositoryState.connectionState.isConnected
                            }

                    DialogOperationControlState.NotRunning(
                        onLaunch = ::launch,
                        onClose = onClose,
                        canLaunch = anyOperationSelected && candidateSelectedAndValid,
                    )
                }
            }
        }

        fun launch() {
            if (state !is ReadyAddPushProcess) {
                throw IllegalStateException("Not ready")
            }

            state.launch(
                LaunchOptions(
                    selectedFilenames.value,
                    selectedMoves.value,
                    selectedDestinationRepositories.value,
                ),
            )
        }

        override fun dialogWidthModifier(): Modifier = fullWidthDialogWidthModifier
    }

    override fun onClose() {
        _onClose()
    }

    fun cancel() {
        currentOperation.value?.cancel()
    }
}

@Composable
fun rememberAddAndPushDialogViewModel(
    scope: CoroutineScope,
    repositoryURI: RepositoryURI,
    onClose: () -> Unit,
): AddAndPushRepoDialogViewModel {
    val storageService = LocalStorageService.current
    val repositoryService = LocalRepoService.current
    val addPushOperationService = LocalAddPushService.current

    val vm =
        remember(scope, repositoryURI) {
            AddAndPushRepoDialogViewModel(
                scope,
                storageService,
                repositoryService,
                repositoryURI,
                addPushOperationService.getAddAndPushProcedure(repositoryURI),
                onClose,
            )
        }

    return vm
}

@Suppress("UNCHECKED_CAST")
fun <T1, T2, T3, T4, T5, T6, R> combine6(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
): Flow<R> =
    combine(flow, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
        )
    }
