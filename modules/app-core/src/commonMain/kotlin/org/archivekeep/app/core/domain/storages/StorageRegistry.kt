package org.archivekeep.app.core.domain.storages

import org.archivekeep.app.core.persistence.registry.RegisteredStorage
import org.archivekeep.app.core.utils.identifiers.StorageURI

interface StorageRegistry {
    suspend fun getStorageByURI(storageURI: StorageURI): RegisteredStorage?
}
