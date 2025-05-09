package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegisteredStorage
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.flows.logCollectionLoadableFlow
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrFailure
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.stateIn

class KnownStorageService(
    val scope: CoroutineScope,
    val dataStore: RegistryDataStore,
    val fileStores: FileStores,
) : StorageRegistry {
    @OptIn(ExperimentalCoroutinesApi::class)
    val knownStorages: Flow<Loadable<List<KnownStorage>>> =
        dataStore
            .registeredRepositories
            .flatMapLatest { repositories ->
                dataStore
                    .registeredStorages
                    .mapLoadedData { registeredStorages ->
                        listOf(
                            repositories.map { it.storageURI },
                            registeredStorages.map { it.uri },
                        ).flatten()
                            .toSet()
                            .map { storageURI ->
                                KnownStorage(
                                    storageURI = storageURI,
                                    registeredStorage = registeredStorages.firstOrNull { it.uri == storageURI },
                                    registeredRepositories = repositories.filter { it.storageURI == storageURI },
                                )
                            }
                    }
            }.logCollectionLoadableFlow("Loaded known storages")
            .stateIn(scope)

    val knownStorageURIs =
        knownStorages
            .mapLoadedData { it.map { s -> s.storageURI }.toSet() }
            .stateIn(scope)

    fun storage(storageURI: StorageURI) = knownStorages.mapLoadedData { it.first { storage -> storage.storageURI == storageURI } }

    override suspend fun getStorageByURI(storageURI: StorageURI): RegisteredStorage? =
        knownStorages
            .firstLoadedOrFailure()
            .firstOrNull {
                it.storageURI == storageURI
            }?.registeredStorage
}
