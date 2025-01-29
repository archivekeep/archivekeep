package org.archivekeep.app.desktop.ui.dialogs.addpush

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.LaunchOptions
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.LaunchedAddPushProcess
import org.archivekeep.app.core.operations.addpush.AddAndPushOperation.ReadyAddPushProcess
import org.archivekeep.app.core.utils.generics.mapToLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.desktop.domain.data.getSyncCandidates
import org.archivekeep.app.desktop.domain.wiring.LocalAddPushService
import org.archivekeep.app.desktop.domain.wiring.LocalRepoService
import org.archivekeep.app.desktop.domain.wiring.LocalStorageService
import org.archivekeep.app.desktop.utils.stickToFirstNotNullAsState
import org.archivekeep.utils.Loadable
import org.archivekeep.utils.mapIfLoadedOrDefault

class AddAndPushDialogViewModel(
    val scope: CoroutineScope,
    val storageService: StorageService,
    val repositoryService: RepositoryService,
    val repositoryURI: RepositoryURI,
    val addPushStatus: AddAndPushOperation,
    val onClose: () -> Unit,
) {
    val selectedDestinationRepositories: MutableStateFlow<Set<RepositoryURI>> = MutableStateFlow(emptySet())
    val selectedFilenames: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    val repoName = repositoryService.getRepository(repositoryURI).informationFlow.map { it.displayName }

    val currentOperation = addPushStatus.currentJobFlow.stickToFirstNotNullAsState(scope)

    val otherRepositoryCandidates = getSyncCandidates(storageService, repositoryURI).mapToLoadable()

    val allNewFilesFlow =
        addPushStatus.stateFlow
            .transform {
                if (it is ReadyAddPushProcess) {
                    emit(it.indexStatus.newFiles)
                }
            }.onStart {
                emit(emptyList())
            }

    val currentStatusFlow =
        currentOperation.flatMapLatest {
            val operationStatus = it?.state

            operationStatus ?: addPushStatus.stateFlow
        }

    val currentVMState =
        combine(
            currentStatusFlow,
//            repoName,
            allNewFilesFlow,
            selectedDestinationRepositories,
            selectedFilenames,
            otherRepositoryCandidates.onEach { v ->
                if (v is Loadable.Loaded) {
                    selectedDestinationRepositories.value =
                        v.value
                            .filter { it.repositoryState.connectionState.isConnected }
                            .map { it.uri }
                            .toSet()
                }
            },
            transform = AddAndPushDialogViewModel::VMState,
        )

    data class VMState(
        val state: AddAndPushOperation.State,
//        val repoName: String,
        val allNewFiles: List<String>,
        val selectedDestinationRepositories: Set<RepositoryURI>,
        val selectedFilenames: Set<String>,
        val otherRepositoryCandidates: Loadable<List<StorageRepository>>,
    ) {
        val showLaunch =
            run {
                state !is LaunchedAddPushProcess || !state.finished
            }

        val canLaunch by derivedStateOf {
            val candidates = otherRepositoryCandidates.mapIfLoadedOrDefault(emptyList()) { it }
            val selections = selectedDestinationRepositories

            state is ReadyAddPushProcess &&
                selectedFilenames.isNotEmpty() &&
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
