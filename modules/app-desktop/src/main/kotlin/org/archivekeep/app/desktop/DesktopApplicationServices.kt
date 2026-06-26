package org.archivekeep.app.desktop

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.archivekeep.app.core.domain.CoreApplicationServiceScope
import org.archivekeep.app.core.domain.CoreApplicationServicesGraph
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.credentials.WalletPO
import org.archivekeep.app.core.persistence.drivers.filesystem.DesktopFileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.registry.PreferenceDataStoreRegistryData
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepositoryInDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepositoryInDataStore
import org.archivekeep.app.ui.domain.services.DesktopRepositoryOpenService
import org.archivekeep.app.ui.domain.services.RepositoryOpenService
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.utils.ApplicationMetadata
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedJoseStorageInFile
import org.archivekeep.utils.datastore.passwordprotected.ProtectedDataStore

@DependencyGraph(AppScope::class, additionalScopes = [CoreApplicationServiceScope::class])
interface DesktopApplicationServices :
    CoreApplicationServicesGraph,
    ApplicationServices {
    @Provides
    @SingleIn(AppScope::class)
    fun registry(scope: CoroutineScope): RegistryDataStore =
        PreferenceDataStoreRegistryData(
            scope,
            getRegistryDatastorePath().toFile(),
        )

    @Provides
    @SingleIn(AppScope::class)
    fun walletDataStore(scope: CoroutineScope): ProtectedDataStore<WalletPO> =
        PasswordProtectedJoseStorageInFile(
            getWalletDatastorePath(),
            Json.serializersModule.serializer(),
            defaultValueProducer = { WalletPO(emptySet()) },
        )

    @Binds
    val DesktopFileStores.bind: FileStores

    @Provides
    @SingleIn(AppScope::class)
    fun repositoryOpenService(fileSystemStorageDriver: FileSystemStorageDriver): RepositoryOpenService = DesktopRepositoryOpenService(fileSystemStorageDriver)

    @Binds
    val PropertiesApplicationMetadata.bind: ApplicationMetadata

    @Provides
    @SingleIn(AppScope::class)
    fun repositoryIndexMemory(scope: CoroutineScope): MemorizedRepositoryIndexRepository =
        MemorizedRepositoryIndexRepositoryInDataStore(scope, getRepositoryIndexMemoryDatastorePath().toFile())

    @Provides
    @SingleIn(AppScope::class)
    fun repositoryMetadataMemory(scope: CoroutineScope): MemorizedRepositoryMetadataRepository =
        MemorizedRepositoryMetadataRepositoryInDataStore(scope, getRepositoryMetadataMemoryDatastorePath().toFile())

    @Provides
    @SingleIn(AppScope::class)
    fun filesystemStorageDriver(
        scope: CoroutineScope,
        fileStores: FileStores,
        credentialsStore: CredentialsStore,
    ): FileSystemStorageDriver = FileSystemStorageDriver(scope, fileStores, credentialsStore)

    @Binds
    @IntoSet
    val FileSystemStorageDriver.bind: StorageDriver

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides scope: CoroutineScope,
            @Provides serviceWorkDispatcher: CoroutineDispatcher,
        ): DesktopApplicationServices
    }
}
