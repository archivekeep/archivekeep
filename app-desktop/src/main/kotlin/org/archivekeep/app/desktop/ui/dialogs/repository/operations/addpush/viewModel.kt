package org.archivekeep.app.desktop.ui.dialogs.repository.operations.addpush

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.LaunchOptions
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.LaunchedAddPushProcess
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.ReadyAddPushProcess
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.data.getSyncCandidates
import org.archivekeep.app.desktop.domain.wiring.LocalAddPushService
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.utils.stickToFirstNotNullAsState
import org.archivekeep.files.operations.AddOperation
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.loading.mapToLoadable

class AddAndPushDialogViewModel(
    val scope: CoroutineScope,
    val storageService: StorageService,
    val repositoryService: RepositoryService,
    val repositoryURI: RepositoryURI,
    val addAndPushOperation: AddAndPushOperation,
    val onClose: () -> Unit,
) {
    val selectedDestinationRepositories: MutableStateFlow<Set<RepositoryURI>> = MutableStateFlow(emptySet())
    val selectedFilenames: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val selectedMoves: MutableStateFlow<Set<AddOperation.PreparationResult.Move>> = MutableStateFlow(emptySet())

    val repoName = repositoryService.getRepository(repositoryURI).informationFlow.map { it.displayName }

    val currentOperation = addAndPushOperation.currentJobFlow.stickToFirstNotNullAsState(scope)

    val otherRepositoryCandidates = getSyncCandidates(storageService, repositoryURI).mapToLoadable()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentStatusFlow =
        currentOperation.flatMapLatest {
            it?.state
                ?: addAndPushOperation.prepare().onStart { emit(AddAndPushOperation.NotReadyAddPushProcess) }
        }

    val currentVMState =
        combine(
            currentStatusFlow,
//            repoName,
            selectedDestinationRepositories,
            selectedFilenames,
            selectedMoves,
            otherRepositoryCandidates.onEach { v ->
                if (v is Loadable.Loaded) {
                    selectedDestinationRepositories.value =
                        v.value
                            .filter { it.repositoryState.connectionState.isConnected }
                            .map { it.uri }
                            .toSet()
                }
            },
        ) { currentStatus, selectedDestinationRepositories, selectedFilenames, selectedMoves, otherRepositoryCandidates ->
            VMState(
                currentStatus,
                selectedDestinationRepositories,
                selectedFilenames,
                selectedMoves,
                otherRepositoryCandidates,
            )
        }

    data class VMState(
        val state: AddAndPushOperation.State,
//        val repoName: String,
        val selectedDestinationRepositories: Set<RepositoryURI>,
        val selectedFilenames: Set<String>,
        val selectedMoves: Set<AddOperation.PreparationResult.Move>,
        val otherRepositoryCandidates: Loadable<List<StorageRepository>>,
    ) {
        val showLaunch =
            run {
                state !is LaunchedAddPushProcess || !state.finished
            }

        val canLaunch by derivedStateOf {
            val candidates = otherRepositoryCandidates.mapIfLoadedOrDefault(emptyList()) { it }
            val selections = selectedDestinationRepositories

            val anyOperationSelected = selectedFilenames.isNotEmpty() || selectedMoves.isNotEmpty()

            state is ReadyAddPushProcess &&
                anyOperationSelected &&
                selections.isNotEmpty() &&
                selections.all { selection ->
                    candidates
                        .first { it.uri == selection }
                        .repositoryState.connectionState.isConnected
                }
        }

        val canStop = state is LaunchedAddPushProcess && !state.finished

        val canHide = state is LaunchedAddPushProcess && !state.finished

        fun launch() {
            if (state !is ReadyAddPushProcess) {
                throw IllegalStateException("Not ready")
            }

            state.launch(
                LaunchOptions(
                    selectedFilenames,
                    selectedMoves,
                    selectedDestinationRepositories,
                ),
            )
        }
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
): AddAndPushDialogViewModel {
    val storageService = LocalStorageService.current
    val repositoryService = LocalRepoService.current
    val addPushOperationService = LocalAddPushService.current

    val vm =
        remember(scope, repositoryURI) {
            AddAndPushDialogViewModel(
                scope,
                storageService,
                repositoryService,
                repositoryURI,
                addPushOperationService.getAddPushOperation(repositoryURI),
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
