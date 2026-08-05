package org.archivekeep.app.ui.views.home.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.archivekeep.app.core.domain.archives.AssociatedArchive
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.repositories.ResolvedRepositoryState
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.domain.storages.StoragePartiallyResolved
import org.archivekeep.app.core.procedures.add.IndexUpdateProcedureSupervisorService
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedureService
import org.archivekeep.app.core.procedures.sync.RepoToRepoSyncService
import org.archivekeep.app.core.utils.identifiers.NamedRepositoryReference
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.isLoading
import org.archivekeep.utils.loading.optional.mapIfLoadedOrNull
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.safeCombine

class HomeLocalArchiveModel(
    scope: CoroutineScope,
    addAndPushProcedureService: AddAndPushProcedureService,
    indexUpdateProcedureSupervisorService: IndexUpdateProcedureSupervisorService,
    repoToRepoSyncService: RepoToRepoSyncService,
    repositoryService: RepositoryService,
    val archive: AssociatedArchive,
    resolvedRepositoryState: ResolvedRepositoryState,
    storage: StoragePartiallyResolved,
) {
    val isAssociated = archive.associationId != null

    val repository = repositoryService.getRepository(resolvedRepositoryState.uri)

    val displayName = resolvedRepositoryState.displayName

    val primaryRepository =
        PrimaryRepositoryDetails(
            resolvedRepositoryState.namedReference,
            storage.namedReference,
        )

    val otherRepositories: List<Pair<StoragePartiallyResolved, RepositoryItemModel>> =
        archive.repositories
            .filter { it.second.uri != resolvedRepositoryState.uri }
            .map { (storage, repo) ->
                Pair(
                    storage,
                    RepositoryItemModel(
                        resolvedRepositoryState.uri,
                        repo,
                        repository = repositoryService.getRepository(repo.namedReference.uri),
                    ),
                )
            }

    val addPushOperation = addAndPushProcedureService.getAddAndPushProcedure(primaryRepository.reference.uri)

    val addOperation = indexUpdateProcedureSupervisorService.getAddOperation(primaryRepository.reference.uri)

    val secondaryRepositories: StateFlow<List<Pair<StoragePartiallyResolved, RepositoryItemUiState>>> =
        safeCombine(
            otherRepositories.map { (storage, secondaryArchiveRepository) ->
                secondaryArchiveRepository.stateFlow(scope, repoToRepoSyncService).map { Pair(storage, it) }
            },
        ) {
            it.toList()
        }.stateIn(scope, SharingStarted.WhileSubscribed(), emptyList())

    val state: StateFlow<HomeLocalArchiveUiState> =
        combine(
            repository.localRepoStatus,
            repository.optionalAccessorFlow,
            secondaryRepositories,
            addPushOperation.currentJobFlow.map { it != null },
            addOperation.currentJobFlow.map { it != null },
        ) { indexStatus, accessor, nonLocalRepositories, addPushOperationRunning, addOperationRunning ->
            HomeLocalArchiveUiState(
                repositoryAccessState = accessor,
                localRepoStatus = indexStatus.mapLoadedData { it.summary },
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
                        },
                addPushOperationRunning = addPushOperationRunning,
                addOperationRunning = addOperationRunning,
                isAssociated = isAssociated,
            )
        }.stateIn(
            scope,
            SharingStarted.WhileSubscribed(),
            HomeLocalArchiveUiState(
                repositoryAccessState = OptionalLoadable.Loading,
                localRepoStatus = OptionalLoadable.Loading,
                canPush = Loadable.Loading,
                anySecondaryAvailable = false,
                loading = true,
                indexStatusText = OptionalLoadable.Loading,
                addPushOperationRunning = false,
                addOperationRunning = false,
                isAssociated = isAssociated,
            ),
        )

    data class PrimaryRepositoryDetails(
        val reference: NamedRepositoryReference,
        val storageReference: StorageNamedReference,
    )
}
