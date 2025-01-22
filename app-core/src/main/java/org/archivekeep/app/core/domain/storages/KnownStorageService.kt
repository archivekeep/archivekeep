package org.archivekeep.app.core.domain.storages

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegisteredStorage
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.generics.firstLoadedOrFailure
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.utils.Loadable

class KnownStorageService(
    val dataStore: RegistryDataStore,
    val fileStores: FileStores,
) : StorageRegistry {
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

    override suspend fun getStorageForPath(path: String): RegisteredStorage? {
        val fileStorage = fileStores.getFileSystemForPath(path) ?: return null

        return knownStorages
            .firstLoadedOrFailure()
            .firstOrNull {
                it.storageURI == fileStorage.storageURI
            }?.registeredStorage
    }
}
