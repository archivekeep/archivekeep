package org.archivekeep.app.core.persistence.platform.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.CredentialsInProtectedDataStore
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.drivers.grpc.GRPCStorageDriver
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.persistence.registry.PreferenceDataStoreRegistryData
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepositoryInDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepositoryInDataStore
import org.archivekeep.app.core.utils.environment.getWalletDatastorePath

class DesktopEnvironment(
    val scope: CoroutineScope,
    override val fileStores: FileStores = FileStores(scope),
) : Environment {
    override val registry: PreferenceDataStoreRegistryData = PreferenceDataStoreRegistryData(scope)

    override val walletDataStore =
        JoseStorage(
            getWalletDatastorePath(),
            Json.serializersModule.serializer(),
            defaultValueProducer = { Credentials(emptySet()) },
        )

    val credentialsStore: CredentialsStore = CredentialsInProtectedDataStore(walletDataStore)

    override val repositoryIndexMemory = MemorizedRepositoryIndexRepositoryInDataStore(scope)
    override val repositoryMetadataMemory = MemorizedRepositoryMetadataRepositoryInDataStore(scope)

    override val storageDrivers =
        mapOf(
            "filesystem" to FileSystemStorageDriver(scope, fileStores),
            "grpc" to GRPCStorageDriver(credentialsStore),
        )
}
