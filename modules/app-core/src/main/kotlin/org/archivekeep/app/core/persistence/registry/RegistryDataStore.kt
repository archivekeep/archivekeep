package org.archivekeep.app.core.persistence.registry

import kotlinx.coroutines.flow.SharedFlow
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.utils.loading.Loadable

interface RegistryDataStore {
    val registeredRepositories: SharedFlow<Set<RegisteredRepository>>
    val registeredStorages: SharedFlow<Loadable<Set<RegisteredStorage>>>

    suspend fun updateStorage(
        uri: StorageURI,
        transform: (storage: RegisteredStorage) -> RegisteredStorage,
    )

    suspend fun updateRepositories(fn: (old: Set<RegisteredRepository>) -> Set<RegisteredRepository>)
}
