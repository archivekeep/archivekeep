package org.archivekeep.app.android

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.archivekeep.app.core.persistence.credentials.CredentialsInProtectedWalletDataStore
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.credentials.WalletPO
import org.archivekeep.app.core.persistence.drivers.filesystem.AndroidFileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.persistence.registry.PreferenceDataStoreRegistryData
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepositoryInDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepositoryInDataStore
import java.util.concurrent.Executor

class AndroidEnvironment(
    context: Context,
    executor: Executor,
    scope: CoroutineScope,
    paths: AndroidEnvironmentPaths,
) : Environment {
    override val fileStores: FileStores = AndroidFileStores(context, scope, executor)

    override val registry: PreferenceDataStoreRegistryData = PreferenceDataStoreRegistryData(scope, paths.getRegistryDatastoreFile())

    override val walletDataStore =
        SecretKeyProtectedDataStore(
            paths.getWalletDatastoreFile().toPath(),
            Json.serializersModule.serializer(),
            keyAlias = "Credentials Wallet Key",
            defaultValueProducer = { WalletPO(emptySet()) },
        )

    override val credentialsStore: CredentialsStore = CredentialsInProtectedWalletDataStore(walletDataStore)

    override val repositoryIndexMemory = MemorizedRepositoryIndexRepositoryInDataStore(scope, paths.getRepositoryIndexMemoryDatastoreFile())
    override val repositoryMetadataMemory = MemorizedRepositoryMetadataRepositoryInDataStore(scope, paths.getRepositoryMetadataMemoryDatastoreFile())

    override val storageDrivers = listOf(FileSystemStorageDriver(scope, fileStores, credentialsStore))
}
