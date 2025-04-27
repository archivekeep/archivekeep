package org.archivekeep.app.ui.views.home

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.archives.ArchiveService
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.addpush.AddAndPushOperationService
import org.archivekeep.app.core.operations.sync.RepoToRepoSyncService
import org.archivekeep.utils.combineToObject
import org.archivekeep.utils.loading.Loadable
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
    val addAndPushOperationService: AddAndPushOperationService,
) {
    val allLocalArchivesFlow =
        archiveService.allArchives
            .mapLoadedData {
                it
                    .mapNotNull { a ->
                        val (storage, primaryRepository) =
                            a.primaryRepository ?: return@mapNotNull null

                        HomeArchiveEntryViewModel(
                            scope,
                            addAndPushOperationService,
                            repoToRepoSyncService,
                            repositoryService.getRepository(primaryRepository.uri),
                            archive = a,
                            primaryRepository.displayName,
                            primaryRepository =
                                HomeArchiveEntryViewModel.PrimaryRepositoryDetails(
                                    primaryRepository.namedReference,
                                    storage.namedReference,
                                    stats = mutableStateOf(Loadable.Loading),
                                ),
                            otherRepositories =
                                a.repositories
                                    .filter { it.second.uri != primaryRepository.uri }
                                    .map { (storage, repo) ->
                                        Pair(
                                            storage,
                                            SecondaryArchiveRepository(
                                                primaryRepository.uri,
                                                repo,
                                                repository = repositoryService.getRepository(repo.namedReference.uri),
                                            ),
                                        )
                                    },
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

                                HomeViewStorage(
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
                                                        SecondaryArchiveRepository(
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
                            }
                    }
            }.flatMapLatestLoadedData { unsortedList ->
                val withState =
                    unsortedList.map { v ->
                        v.storage.state.map { Pair(it, v) }
                    }

                combineToObject(withState) { storages ->
                    HomeStoragesState(
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

                    HomeArchiveNonLocalArchive(
                        a,
                        a.repositories[0].second.displayName,
                        otherRepositories =
                            a.repositories
                                .map { (storage, repo) ->
                                    HomeArchiveNonLocalArchive.OtherRepositoryDetails(
                                        repo.namedReference,
                                        storage.namedReference,
                                        repositoryService.getRepository(repo.namedReference.uri),
                                    )
                                },
                    )
                }.sortedBy { it.displayName }
        }
}
