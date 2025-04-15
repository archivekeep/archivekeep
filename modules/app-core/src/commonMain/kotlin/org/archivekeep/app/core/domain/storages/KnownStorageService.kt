package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegisteredStorage
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.StorageURI
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
        dataStore.registeredRepositories
            .flatMapLatest { repositories ->
                println("Repositories: $repositories")

                dataStore.registeredStorages
                    .mapLoadedData { registeredStorages ->
                        listOf(
                            repositories.map { it.storageURI }.onEach { println("Repo URI: $it") },
                            registeredStorages.map { it.uri }.onEach { println("Registered URI: $it") },
                        ).flatten()
                            .toSet()
                            .map { storageURI ->
                                KnownStorage(
                                    storageURI = storageURI,
                                    registeredStorage = registeredStorages.firstOrNull { it.uri == storageURI },
                                    registeredRepositories = repositories.filter { it.storageURI == storageURI },
                                )
                            }
                    }.onEach {
                        println("Storages: $it")
                    }
            }

    val knownStorageURIs =
        knownStorages
            .mapLoadedData { it.map { s -> s.storageURI }.toSet() }
            .stateIn(scope)

    fun storage(storageURI: StorageURI) = knownStorages.mapLoadedData { it.first { storage -> storage.storageURI == storageURI } }

    override suspend fun getStorageForPath(path: String): RegisteredStorage? {
        val fileStorage = fileStores.getFileSystemForPath(path) ?: return null

        return knownStorages
            .firstLoadedOrFailure()
            .firstOrNull {
                it.storageURI == fileStorage.storageURI
            }?.registeredStorage
    }
}
