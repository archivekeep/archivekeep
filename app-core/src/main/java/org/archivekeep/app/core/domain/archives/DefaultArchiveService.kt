package org.archivekeep.app.core.domain.archives

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.utils.generics.flatMapLatestLoadedData
import org.archivekeep.app.core.utils.generics.sharedWhileSubscribed
import org.archivekeep.utils.combineToList

class DefaultArchiveService(
    scope: CoroutineScope,
    storageService: StorageService,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ArchiveService {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val allArchives =
        storageService
            .allStorages
            .flatMapLatestLoadedData { allStorages ->
                val repositoriesFlows =
                    allStorages.map { it.repositories }

                combineToList(repositoriesFlows) { storageRepos ->
                    val repos = storageRepos.flatMap { it }
                    val allCorrelationId = repos.map { it.associationId }.distinct()

                    val correlatedArchives =
                        allCorrelationId
                            .filterNotNull()
                            .map { cId ->
                                val allRepos =
                                    repos
                                        .filter { it.associationId == cId }
                                        .map { repo ->
                                            Pair(
                                                allStorages.first { it.uri == repo.storage.uri },
                                                repo.repositoryState,
                                            )
                                        }

                                AssociatedArchive(
                                    cId,
                                    repositories = allRepos,
                                )
                            }

                    val uncorrelatedArchives =
                        repos
                            .filter { it.associationId == null }
                            .map { repo ->
                                AssociatedArchive(
                                    null,
                                    repositories =
                                        listOf(
                                            Pair(
                                                allStorages.first { it.uri == repo.storage.uri },
                                                repo.repositoryState,
                                            ),
                                        ),
                                )
                            }

                    correlatedArchives + uncorrelatedArchives
                }
            }.flowOn(computeDispatcher)
            .sharedWhileSubscribed(scope)
}
