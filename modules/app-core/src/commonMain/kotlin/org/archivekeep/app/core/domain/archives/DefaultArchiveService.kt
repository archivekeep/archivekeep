package org.archivekeep.app.core.domain.archives

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import org.archivekeep.app.core.domain.CoreApplicationServiceScope
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.utils.combineToList
import org.archivekeep.utils.loading.flatMapLatestLoadedData
import org.archivekeep.utils.loading.stateIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(CoreApplicationServiceScope::class)
class DefaultArchiveService(
    scope: CoroutineScope,
    storageService: StorageService,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ArchiveService {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val allArchives =
        storageService
            .allStoragesPartiallyResolved
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
            .stateIn(scope)
}
