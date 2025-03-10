package org.archivekeep.app.core.persistence.platform

import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository

interface Environment {
    val walletDataStore: JoseStorage<Credentials>
    val registry: RegistryDataStore
    val repositoryIndexMemory: MemorizedRepositoryIndexRepository
    val repositoryMetadataMemory: MemorizedRepositoryMetadataRepository
    val storageDrivers: Map<String, StorageDriver>

    val fileStores: FileStores
}
