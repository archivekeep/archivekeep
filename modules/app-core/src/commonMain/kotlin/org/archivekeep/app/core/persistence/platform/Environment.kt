package org.archivekeep.app.core.persistence.platform

import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.credentials.WalletPO
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.utils.datastore.passwordprotected.ProtectedDataStore

interface Environment {
    val registry: RegistryDataStore
    val repositoryIndexMemory: MemorizedRepositoryIndexRepository
    val repositoryMetadataMemory: MemorizedRepositoryMetadataRepository
    val storageDrivers: List<StorageDriver>

    val walletDataStore: ProtectedDataStore<WalletPO>
    val credentialsStore: CredentialsStore

    val fileStores: FileStores
}
