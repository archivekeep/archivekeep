package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.utils.generics.UniqueInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.combineToFlatMapList
import org.archivekeep.utils.combineToList
import org.archivekeep.utils.coroutines.sharedResourceInGlobalScope
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.flatMapLatestLoadedData
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.stateIn
import org.archivekeep.utils.loading.waitLoadedValue

class StorageService(
    val scope: CoroutineScope,
    val knownStorageService: KnownStorageService,
    val storageDrivers: Map<String, StorageDriver>,
    val repositoryService: RepositoryService,
) {
    private val storageInstances =
        UniqueInstanceManager(factory = { storageURI: StorageURI ->
            Storage(
                scope,
                repositoryService,
                storageURI,
                knownStorageService.storage(storageURI),
                storageDrivers[storageURI.driver]?.getStorageAccessor(
                    storageURI,
                ) ?: notSupportedStorage(storageURI),
            )
        })

    fun storage(uri: StorageURI) = storageInstances[uri]

    val allStorages =
        knownStorageService
            .knownStorageURIs
            .mapLoadedData(storageInstances::get)
            .stateIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allStoragesPartiallyResolved =
        allStorages
            .flatMapLatestLoadedData { storages ->
                combineToList(
                    storages.map { storage ->
                        // TODO: get rid of waitLoadedValue - don't make list depend on resolution of all instances
                        storage.partiallyResolved.waitLoadedValue()
                    },
                )
            }.stateIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRepos =
        allStoragesPartiallyResolved
            .flatMapLatestLoadedData {
                combineToFlatMapList(it.map { it.repositories })
            }.stateIn(scope)

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
        information = StorageInformation.Error(RuntimeException("Not supported storage driver: $storageURI")),
        connectionStatus =
            flowOf(
                Loadable.Failed(RuntimeException("Not supported storage driver: $storageURI")),
            ).sharedResourceInGlobalScope(),
    )
