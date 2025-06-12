package org.archivekeep.app.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.core.persistence.drivers.filesystem.DesktopFileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.persistence.registry.PreferenceDataStoreRegistryData
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepositoryInDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepositoryInDataStore

class DesktopEnvironment(
    val scope: CoroutineScope,
) : Environment {
    override val fileStores: FileStores = DesktopFileStores(scope)

    override val registry: PreferenceDataStoreRegistryData =
        PreferenceDataStoreRegistryData(
            scope,
            getRegistryDatastorePath().toFile(),
        )

    override val walletDataStore =
        JoseStorage(
            getWalletDatastorePath(),
            Json.serializersModule.serializer(),
            defaultValueProducer = { Credentials(emptySet()) },
        )

    override val repositoryIndexMemory = MemorizedRepositoryIndexRepositoryInDataStore(scope, getRepositoryIndexMemoryDatastorePath().toFile())
    override val repositoryMetadataMemory = MemorizedRepositoryMetadataRepositoryInDataStore(scope, getRepositoryMetadataMemoryDatastorePath().toFile())

    override val storageDrivers = listOf(FileSystemStorageDriver(scope, fileStores))
}
