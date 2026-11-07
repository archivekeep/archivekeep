package org.archivekeep.app.android

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.archivekeep.app.core.domain.CoreApplicationServiceScope
import org.archivekeep.app.core.domain.CoreApplicationServicesGraph
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.credentials.WalletPO
import org.archivekeep.app.core.persistence.drivers.filesystem.AndroidFileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.registry.PreferenceDataStoreRegistryData
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepositoryInDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepositoryInDataStore
import org.archivekeep.app.ui.domain.services.AndroidRepositoryOpenService
import org.archivekeep.app.ui.domain.services.RepositoryOpenService
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.utils.ApplicationMetadata
import org.archivekeep.utils.datastore.passwordprotected.ProtectedDataStore
import java.util.concurrent.Executor

@DependencyGraph(AppScope::class, additionalScopes = [CoreApplicationServiceScope::class])
interface AndroidApplicationServices :
    CoreApplicationServicesGraph,
    ApplicationServices {
    @Binds
    val AndroidFileStores.bind: FileStores

    @Binds
    val AndroidApplicationMetadata.bind: ApplicationMetadata

    @Binds
    val AndroidRepositoryOpenService.bind: RepositoryOpenService

    @Provides
    @SingleIn(AppScope::class)
    fun registry(
        scope: CoroutineScope,
        paths: AndroidEnvironmentPaths,
    ): RegistryDataStore = PreferenceDataStoreRegistryData(scope, paths.getRegistryDatastoreFile())

    @Provides
    @SingleIn(AppScope::class)
    fun walletDataStore(
        scope: CoroutineScope,
        paths: AndroidEnvironmentPaths,
    ): ProtectedDataStore<WalletPO> =
        SecretKeyProtectedDataStore(
            paths.getWalletDatastoreFile().toPath(),
            Json.serializersModule.serializer(),
            keyAlias = "Credentials Wallet Key",
            defaultValueProducer = { WalletPO(emptySet()) },
        )

    @Provides
    @SingleIn(AppScope::class)
    fun repositoryIndexMemory(
        scope: CoroutineScope,
        paths: AndroidEnvironmentPaths,
    ): MemorizedRepositoryIndexRepository = MemorizedRepositoryIndexRepositoryInDataStore(scope, paths.getRepositoryIndexMemoryDatastoreFile())

    @Provides
    @SingleIn(AppScope::class)
    fun repositoryMetadataMemory(
        scope: CoroutineScope,
        paths: AndroidEnvironmentPaths,
    ): MemorizedRepositoryMetadataRepository = MemorizedRepositoryMetadataRepositoryInDataStore(scope, paths.getRepositoryMetadataMemoryDatastoreFile())

    @Provides
    @SingleIn(AppScope::class)
    @IntoSet
    fun filesystemStorageDriver(
        scope: CoroutineScope,
        fileStores: FileStores,
        credentialsStore: CredentialsStore,
    ): StorageDriver = FileSystemStorageDriver(scope, fileStores, credentialsStore)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides context: Context,
            @Provides executor: Executor,
            @Provides serviceWorkDispatcher: CoroutineDispatcher,
            @Provides scope: CoroutineScope,
            @Provides paths: AndroidEnvironmentPaths,
        ): AndroidApplicationServices
    }
}
