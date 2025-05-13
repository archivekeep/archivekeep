package org.archivekeep.app.ui.views.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.domain.archives.AssociatedArchive
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.domain.storages.StoragePartiallyResolved
import org.archivekeep.app.core.operations.addpush.AddAndPushOperationService
import org.archivekeep.app.core.operations.sync.RepoToRepoSyncService
import org.archivekeep.app.core.utils.generics.isLoading
import org.archivekeep.app.core.utils.generics.mapIfLoadedOrNull
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.generics.mapToLoadable
import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference
import org.archivekeep.app.ui.domain.wiring.ArchiveOperationLaunchers
import org.archivekeep.app.ui.enableUnfinishedFeatures
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.safeCombine

class HomeArchiveEntryViewModel(
    scope: CoroutineScope,
    addAndPushOperationService: AddAndPushOperationService,
    repoToRepoSyncService: RepoToRepoSyncService,
    val repository: Repository,
    val archive: AssociatedArchive,
    val displayName: String,
    val primaryRepository: PrimaryRepositoryDetails,
    val otherRepositories: List<Pair<StoragePartiallyResolved, SecondaryArchiveRepository>>,
) {
    data class VMState(
        val canAdd: Loadable<Boolean>,
        val canPush: Loadable<Boolean>,
        val anySecondaryAvailable: Boolean,
        val loading: Boolean,
        val indexStatusText: Loadable<String>,
        val addPushOperationRunning: Boolean,
    ) {
        val canAddPush = if (addPushOperationRunning) Loadable.Loaded(true) else (canAdd.mapLoadedData { it && anySecondaryAvailable })
    }

    val addPushOperation = addAndPushOperationService.getAddPushOperation(primaryRepository.reference.uri)

    val secondaryRepositories: StateFlow<List<Pair<StoragePartiallyResolved, SecondaryArchiveRepository.State>>> =
        safeCombine(
            otherRepositories.map { (storage, secondaryArchiveRepository) ->
                secondaryArchiveRepository.stateFlow(scope, repoToRepoSyncService).map { Pair(storage, it) }
            },
        ) {
            it.toList()
        }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    val state: StateFlow<VMState> =
        combine(
            repository.localRepoStatus,
            secondaryRepositories,
            addPushOperation.currentJobFlow.map { it != null },
        ) { indexStatus, nonLocalRepositories, addPushOperationRunning ->
            VMState(
                canAdd = indexStatus.mapLoadedData { it.hasChanges }.mapToLoadable(false),
                canPush =
                    if (nonLocalRepositories.any { it.second.canPushLoadable.mapIfLoadedOrNull { it } ?: false }) {
                        Loadable.Loaded(true)
                    } else {
                        if (nonLocalRepositories.none { it.second.canPushLoadable.isLoading }) {
                            Loadable.Loaded(false)
                        } else {
                            Loadable.Loading
                        }
                    },
                anySecondaryAvailable = nonLocalRepositories.any { it.second.connectionStatus.isAvailable },
                loading = indexStatus.isLoading,
                indexStatusText =
                    indexStatus
                        .mapLoadedData {
                            "${it.indexedFiles.size} files${it.newFiles.size.let { if (it > 0) ", $it uncommitted" else "" }}"
                        }.mapToLoadable {
                            Loadable.Failed(it.cause ?: RuntimeException("Expected status data"))
                        },
                addPushOperationRunning = addPushOperationRunning,
            )
        }.onEach {
            println("Archive state: ${archive.label}: $it")
        }.stateIn(
            scope,
            SharingStarted.WhileSubscribed(),
            VMState(
                canAdd = Loadable.Loading,
                canPush = Loadable.Loading,
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
    val repoToRepoSyncService: RepoToRepoSyncService,
    val storage: StoragePartiallyResolved,
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
        safeCombine(otherRepositoriesInThisStorage.map { it.stateFlow(scope, repoToRepoSyncService) }) {
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

@Composable
fun HomeArchiveEntryViewModel.VMState.actions(
    archiveOperationLaunchers: ArchiveOperationLaunchers,
    localArchive: HomeArchiveEntryViewModel,
): List<Loadable<Action>> =
    listOf(
        this.canAddPush.mapLoadedData {
            Action(
                onLaunch = {
                    archiveOperationLaunchers.openAddAndPushOperation(
                        localArchive.primaryRepository.reference.uri,
                    )
                },
                text = "Add and push",
                isAvailable = it,
                running = this.addPushOperationRunning,
            )
        },
        this.canAdd.mapLoadedData {
            Action(
                onLaunch = {
                    archiveOperationLaunchers.openIndexUpdateOperation(
                        localArchive.primaryRepository.reference.uri,
                    )
                },
                isAvailable = it,
                text = "Add",
            )
        },
        this.canPush.mapLoadedData {
            Action(
                onLaunch = {
                    archiveOperationLaunchers.pushRepoToAll(
                        localArchive.primaryRepository.reference.uri,
                    )
                },
                isAvailable = it && enableUnfinishedFeatures,
                text = "Push to all",
            )
        },
    )
