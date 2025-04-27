package org.archivekeep.app.ui.domain.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.ui.domain.wiring.LocalStorageService
import org.archivekeep.app.ui.utils.collectAsLoadableState
import org.archivekeep.utils.combineToFlatMapList
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.flatMapLatestLoadedData
import org.archivekeep.utils.loading.waitLoadedValue

@OptIn(ExperimentalCoroutinesApi::class)
fun getSyncCandidates(
    storageService: StorageService,
    repositoryURI: RepositoryURI,
): Flow<List<StorageRepository>> =
    storageService.allStoragesPartiallyResolved
        .flatMapLatestLoadedData { storages ->
            val allReposFlow = combineToFlatMapList(storages.map { s -> s.repositories })

            allReposFlow.map { allRepos ->
                val currentRepo = allRepos.first { it.uri == repositoryURI }

                allRepos
                    .filter { it.associationId == currentRepo.associationId }
                    .filter { it.uri != repositoryURI }
                    .filter { it.repositoryState.connectionState.isAvailable }
            }
        }.waitLoadedValue()

@Composable
fun getSyncCandidatesAsStateFlow(repositoryURI: RepositoryURI): State<Loadable<List<StorageRepository>>> {
    val storageService = LocalStorageService.current

    val reposFlow =
        remember(storageService, repositoryURI) {
            getSyncCandidates(storageService, repositoryURI)
        }.collectAsLoadableState()

    return reposFlow
}
