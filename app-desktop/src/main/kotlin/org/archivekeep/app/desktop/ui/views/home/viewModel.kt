package org.archivekeep.app.desktop.ui.views.home

import androidx.compose.runtime.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.domain.archives.AssociatedArchive
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.operations.addpush.AddPushOperationService
import org.archivekeep.app.core.operations.addpush.RepoAddPush
import org.archivekeep.app.core.operations.derived.SyncService
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference
import org.archivekeep.utils.Loadable
import org.archivekeep.utils.isLoading
import org.archivekeep.utils.mapIfLoadedOrDefault
import org.archivekeep.utils.safeCombine

class HomeArchiveEntryViewModel(
    scope: CoroutineScope,
    addPushOperationService: AddPushOperationService,
    syncService: SyncService,
    val repository: Repository,
    val archive: AssociatedArchive,
    val displayName: String,
    val primaryRepository: PrimaryRepositoryDetails,
    val otherRepositories: List<Pair<Storage, SecondaryArchiveRepository>>,
) {
    data class VMState(
        val canAdd: Boolean,
        val canPush: Boolean,
        val anySecondaryAvailable: Boolean,
        val loading: Boolean,
        val indexStatusText: Loadable<String>,
        val addPushOperationRunning: Boolean,
    ) {
        val canAddPush = canAdd && anySecondaryAvailable
    }

    val addPushOperation = addPushOperationService.getAddPushOperation(primaryRepository.reference.uri)

    val secondaryRepositories: StateFlow<List<Pair<Storage, SecondaryArchiveRepository.State>>> =
        safeCombine(
            otherRepositories.map { (storage, secondaryArchiveRepository) ->
                secondaryArchiveRepository.stateFlow(scope, syncService).map { Pair(storage, it) }
            },
        ) {
            it.toList()
        }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    val state: StateFlow<VMState> =
        combine(
            repository.localRepoStatus,
            secondaryRepositories,
            addPushOperation.stateFlow.map { it is RepoAddPush.LaunchedAddPushProcess }.onStart { emit(false) },
        ) { indexStatus, nonLocalRepositories, addPushOperationRunning ->
            VMState(
                canAdd = indexStatus is Loadable.Loaded && indexStatus.value.hasChanges,
                canPush = nonLocalRepositories.any { it.second.canPush },
                anySecondaryAvailable = nonLocalRepositories.any { it.second.connectionStatus.isAvailable },
                loading = indexStatus.isLoading,
                indexStatusText =
                    indexStatus.mapLoadedData {
                        "${it.storedFiles.size} files${it.newFiles.size.let { if (it > 0) ", $it uncommitted" else "" }}"
                    },
                addPushOperationRunning = addPushOperationRunning,
            )
        }.onEach {
            println("Archive state: ${archive.label}: $it")
        }.stateIn(
            scope,
            SharingStarted.WhileSubscribed(),
            VMState(
                canAdd = false,
                canPush = false,
                anySecondaryAvailable = false,
                loading = true,
                indexStatusText = Loadable.Loading,
                addPushOperationRunning = false,
            ),
        )

    data class PrimaryRepositoryDetails(
        val reference: NamedRepositoryReference,
        val storageReference: StorageNamedReference,
        val stats: State<Loadable<LocalRepositoryStats>>,
    )

    class LocalRepositoryStats(
        val totalFiles: Int,
    )
}

class HomeArchiveNonLocalArchive(
    val archive: AssociatedArchive,
    val displayName: String,
    val otherRepositories: List<OtherRepositoryDetails>,
) {
    class OtherRepositoryDetails(
        val reference: NamedRepositoryReference,
        val storageReference: StorageNamedReference,
        val repository: Repository,
    )
}

class HomeViewStorage(
    scope: CoroutineScope,
    val syncService: SyncService,
    val storage: Storage,
    val reference: StorageNamedReference = storage.namedReference,
    val name: String? = storage.knownStorage.registeredStorage?.label,
    val otherRepositoriesInThisStorage: List<SecondaryArchiveRepository>,
) {
    data class ResolvedState(
        val resolvedRepositories: Loadable<List<SecondaryArchiveRepository.State>>,
        val isConnected: Boolean,
    ) {
        val canPushAny = resolvedRepositories.mapIfLoadedOrDefault(false) { it.any { it.canPush } }
        val canPullAny = resolvedRepositories.mapIfLoadedOrDefault(false) { it.any { it.canPull } }
    }

    val secondaryRepositories: StateFlow<List<SecondaryArchiveRepository.State>> =
        safeCombine(otherRepositoriesInThisStorage.map { it.stateFlow(scope, syncService) }) {
            it.toList()
        }.stateIn(scope, SharingStarted.Lazily, emptyList())

    val stateFlow: StateFlow<ResolvedState> =
        combine(
            secondaryRepositories,
            storage.state,
        ) { secondaryRepositories, storageState ->
            ResolvedState(
                Loadable.Loaded(secondaryRepositories.toList()),
                storageState.mapIfLoadedOrDefault(false) { it.isConnected },
            )
        }.stateIn(scope, SharingStarted.Lazily, ResolvedState(Loadable.Loading, false))
}

data class HomeViewAction(
    val title: String,
    val onTrigger: () -> Unit,
)
