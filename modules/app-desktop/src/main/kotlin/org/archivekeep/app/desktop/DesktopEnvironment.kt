package org.archivekeep.app.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.CredentialsInProtectedDataStore
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.core.persistence.drivers.filesystem.DesktopFileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.drivers.grpc.GRPCStorageDriver
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver
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

    val credentialsStore: CredentialsStore = CredentialsInProtectedDataStore(walletDataStore)

    override val repositoryIndexMemory = MemorizedRepositoryIndexRepositoryInDataStore(scope, getRepositoryIndexMemoryDatastorePath().toFile())
    override val repositoryMetadataMemory = MemorizedRepositoryMetadataRepositoryInDataStore(scope, getRepositoryMetadataMemoryDatastorePath().toFile())

    override val storageDrivers =
        mapOf(
            "filesystem" to FileSystemStorageDriver(scope, fileStores),
            "grpc" to GRPCStorageDriver(scope, credentialsStore),
            "s3" to S3StorageDriver(scope, credentialsStore),
        )
}
