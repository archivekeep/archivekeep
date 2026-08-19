package org.archivekeep.app.ui.views.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.archives.ArchiveService
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.procedures.add.IndexUpdateProcedureSupervisorService
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedureService
import org.archivekeep.app.core.procedures.sync.RepoToRepoSyncService
import org.archivekeep.app.ui.views.home.model.HomeExternalArchiveModel
import org.archivekeep.app.ui.views.home.model.HomeLocalArchiveModel
import org.archivekeep.app.ui.views.home.model.HomeStorageListUiState
import org.archivekeep.app.ui.views.home.model.HomeStorageUiState
import org.archivekeep.app.ui.views.home.model.RepositoryItemModel
import org.archivekeep.utils.combineToObject
import org.archivekeep.utils.loading.flatMapLatestLoadedData
import org.archivekeep.utils.loading.flatMapLoadableFlow
import org.archivekeep.utils.loading.isLoading
import org.archivekeep.utils.loading.mapIfLoadedOrDefault
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.stateIn

class HomeViewModel(
    val scope: CoroutineScope,
    val archiveService: ArchiveService,
    val repositoryService: RepositoryService,
    val storageService: StorageService,
    val repoToRepoSyncService: RepoToRepoSyncService,
    val addAndPushProcedureService: AddAndPushProcedureService,
    val indexUpdateProcedureSupervisorService: IndexUpdateProcedureSupervisorService,
) {
    val allLocalArchivesFlow =
        archiveService.allArchives
            .mapLoadedData {
                it
                    .mapNotNull { archive ->
                        val (storage, primaryRepository) =
                            archive.primaryRepository ?: return@mapNotNull null

                        HomeLocalArchiveModel(
                            scope,
                            addAndPushProcedureService,
                            indexUpdateProcedureSupervisorService,
                            repoToRepoSyncService,
                            repositoryService,
                            archive = archive,
                            resolvedRepositoryState = primaryRepository,
                            storage = storage,
                        )
                    }.sortedBy { it.displayName }
            }.stateIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allStoragesFlow =
        storageService.allStoragesPartiallyResolved
            .flatMapLoadableFlow { allStorages ->
                val nonLocalStorages =
                    allStorages.filter { !it.isLocal }

                archiveService.allArchives
                    .mapLoadedData { allArchives ->
                        nonLocalStorages
                            .map { storage ->
                                val storageReference = storage.namedReference

                                HomeStorageUiState(
                                    scope,
                                    repoToRepoSyncService = repoToRepoSyncService,
                                    storage = storage,
                                    otherRepositoriesInThisStorage =
                                        allArchives
                                            .flatMap { aa ->
                                                aa.repositories
                                                    .filter {
                                                        it.first.uri == storageReference.uri
                                                    }.map { (_, repo) ->
                                                        RepositoryItemModel(
                                                            aa.primaryRepository?.second?.uri,
                                                            repo,
                                                            repository =
                                                                repositoryService.getRepository(
                                                                    repo.namedReference.uri,
                                                                ),
                                                        )
                                                    }
                                            },
                                )
                            }.filter { it.otherRepositoriesInThisStorage.isNotEmpty() }
                    }
            }.flatMapLatestLoadedData { unsortedList ->
                val withState =
                    unsortedList.map { v ->
                        v.storage.state.map { Pair(it, v) }
                    }

                combineToObject(withState) { storages ->
                    HomeStorageListUiState(
                        isLoadingSomeItems = storages.any { it.first.isLoading },
                        hasAnyRegistered = storages.isNotEmpty(),
                        availableStorages =
                            storages
                                .filter { it.first.mapIfLoadedOrDefault(false) { it.isConnected } }
                                .sortedBy { it.second.reference.displayName }
                                .map { it.second },
                    )
                }
            }.stateIn(scope)

    val otherArchivesFlow =
        archiveService.allArchives.mapLoadedData {
            it
                .mapNotNull { a ->
                    if (a.primaryRepository != null) {
                        return@mapNotNull null
                    }

                    HomeExternalArchiveModel(
                        scope,
                        a,
                        a.repositories[0].second.displayName,
                        otherRepositories =
                            a.repositories
                                .map { (storage, repo) ->
                                    HomeExternalArchiveModel.OtherRepositoryDetails(
                                        repo,
                                        storage.namedReference,
                                        repositoryService.getRepository(repo.namedReference.uri),
                                    )
                                },
                    )
                }.sortedBy { it.displayName }
        }
}
