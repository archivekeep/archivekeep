package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.combineToFlatMapList
import org.archivekeep.utils.combineToList
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.coroutines.sharedResourceInGlobalScope
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.flatMapLatestLoadedData
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.waitLoadedValue

class StorageService(
    val scope: CoroutineScope,
    val knownStorageService: KnownStorageService,
    val storageDrivers: Map<String, StorageDriver>,
    val repositoryService: RepositoryService,
) {
    val allStorages =
        knownStorageService
            .knownStorages
            .mapLoadedData { knownStorages ->
                knownStorages.map { knownStorage ->
                    val storageURI = knownStorage.storageURI

                    val storageRef = StorageNamedReference(storageURI, knownStorage.label)

                    Storage(
                        storageURI,
                        knownStorage,
                        storageDrivers[storageURI.driver]?.getStorageAccessor(
                            storageURI,
                        ) ?: notSupportedStorage(storageURI),
                        combineToList(
                            knownStorage.registeredRepositories.map { registeredRepo ->
                                repositoryService
                                    .getRepository(registeredRepo.uri)
                                    .resolvedState
                                    .map {
                                        StorageRepository(
                                            storageRef,
                                            it.uri,
                                            it,
                                        )
                                    }
                            },
                        ).shareResourceIn(scope),
                    )
                }
            }.shareResourceIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRepos =
        allStorages
            .flatMapLatestLoadedData {
                combineToFlatMapList(it.map { it.repositories })
            }.shareResourceIn(scope)

    fun repository(repositoryURI: RepositoryURI): Flow<StorageRepository> =
        allRepos
            .mapLoadedData {
                it.first { it.uri == repositoryURI }
            }.waitLoadedValue()
}

@OptIn(DelicateCoroutinesApi::class)
fun notSupportedStorage(storageURI: StorageURI) =
    StorageConnection(
        storageURI,
        information = flowOf(OptionalLoadable.NotAvailable()),
        connectionStatus =
            flowOf(
                Loadable.Failed(RuntimeException("Not supported storage driver: $storageURI")),
            ).sharedResourceInGlobalScope(),
    )
