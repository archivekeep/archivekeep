package org.archivekeep.app.core.domain.storages

import org.archivekeep.app.core.persistence.registry.RegisteredStorage

interface StorageRegistry {
    suspend fun getStorageForPath(path: String): RegisteredStorage?
}
