package org.archivekeep.app.core.persistence.platform.demo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.archivekeep.app.core.domain.CoreApplicationServiceScope
import org.archivekeep.app.core.domain.CoreApplicationServicesGraph
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.persistence.credentials.WalletPO
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.MountedFileSystem
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedJoseStorageInFile
import org.archivekeep.utils.datastore.passwordprotected.ProtectedDataStore
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.stateIn
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

@DependencyGraph(AppScope::class, additionalScopes = [CoreApplicationServiceScope::class])
interface DemoApplicationServices : CoreApplicationServicesGraph {
    @Provides
    @SingleIn(AppScope::class)
    fun fileStores(
        scope: CoroutineScope,
        mountPoints: List<MountedFileSystem.MountPoint> = emptyList(),
    ): FileStores =
        object : FileStores {
            override val mountPoints: StateFlow<Loadable<List<MountedFileSystem.MountPoint>>> =
                flowOf(mountPoints)
                    .mapToLoadable()
                    .stateIn(scope)

            override val mountedFileSystems =
                this.mountPoints.mapLoadedData { mountPoints ->
                    mountPoints
                        .map { it.fsUUID }
                        .toSet()
                        .map { fsUUID ->
                            val mp = mountPoints.filter { it.fsUUID == fsUUID }
                            val label = mp.map { it.fsLabel }.maxBy { label -> mp.count { it.fsLabel == label } }

                            MountedFileSystem(
                                fsUUID = fsUUID,
                                fsLabel = label.ifBlank { fsUUID },
                                mountPoints = mp,
                            )
                        }
                }

            override suspend fun loadFreshMountPoints(): List<MountedFileSystem.MountPoint> = mountPoints
        }

    @Provides
    @SingleIn(AppScope::class)
    @Named("demoTempDirectory")
    fun demoTempDirectory(): Path = createTempDirectory("archivekeep-demo-env")

    @Provides
    @SingleIn(AppScope::class)
    fun walletDataStore(
        @Named("demoTempDirectory") demoTempDirectory: Path,
    ): PasswordProtectedJoseStorageInFile<WalletPO> =
        PasswordProtectedJoseStorageInFile(
            demoTempDirectory.resolve("credentials.jwe"),
            Json.serializersModule.serializer(),
            defaultValueProducer = { WalletPO(emptySet()) },
        )

    val passwordProtectedWalletDataStore: PasswordProtectedJoseStorageInFile<WalletPO>

    @Binds
    val PasswordProtectedJoseStorageInFile<WalletPO>.bind: ProtectedDataStore<WalletPO>

    @Binds
    val DemoInMemoryRepositories.bind: RegistryDataStore

    @Provides
    @SingleIn(AppScope::class)
    fun repositoryIndexMemory(): MemorizedRepositoryIndexRepository =
        object : MemorizedRepositoryIndexRepository {
            override fun repositoryMemorizedIndexFlow(uri: RepositoryURI): Flow<OptionalLoadable<RepoIndex>> = flowOf(OptionalLoadable.NotAvailable())

            override suspend fun updateRepositoryMemorizedIndexIfDiffers(
                uri: RepositoryURI,
                metadata: RepoIndex?,
            ) {
                // TODO
            }
        }

    @Provides
    @SingleIn(AppScope::class)
    fun repositoryMetadataMemory(): MemorizedRepositoryMetadataRepository =
        object : MemorizedRepositoryMetadataRepository {
            override fun repositoryCachedMetadataFlow(uri: RepositoryURI): Flow<OptionalLoadable<RepositoryMetadata>> {
                // TODO
                return flowOf(OptionalLoadable.NotAvailable())
            }

            override suspend fun updateRepositoryMemorizedMetadataIfDiffers(
                uri: RepositoryURI,
                metadata: RepositoryMetadata?,
            ) {
                // TODO
            }
        }

    @Provides
    @ElementsIntoSet
    fun storageDrivers(
        demoInMemoryRepositories: DemoInMemoryRepositories,
        @Named("storagesOverride") storagesOverride: List<StorageDriver>?,
    ): List<StorageDriver> = storagesOverride ?: listOf(demoInMemoryRepositories)

    @DependencyGraph.Factory
    abstract class Factory {
        abstract fun createBase(
            @Provides scope: CoroutineScope,
            @Provides serviceWorkDispatcher: CoroutineDispatcher,
            @Provides @Named("enableSpeedLimit") enableSpeedLimit: Boolean,
            @Provides physicalMediaData: List<DemoPhysicalMedium>,
            @Provides onlineStoragesData: List<DemoOnlineStorage>,
            @Provides mountPoints: List<MountedFileSystem.MountPoint>,
            @Provides @Named("storagesOverride") storagesOverride: List<StorageDriver>?,
        ): DemoApplicationServices

        fun create(
            scope: CoroutineScope,
            serviceWorkDispatcher: CoroutineDispatcher,
            enableSpeedLimit: Boolean = true,
            physicalMediaData: List<DemoPhysicalMedium> = listOf(LaptopSSD, LaptopHDD, hddB, hddC),
            onlineStoragesData: List<DemoOnlineStorage> = emptyList(),
            mountPoints: List<MountedFileSystem.MountPoint> = emptyList(),
            storagesOverride: List<StorageDriver>? = null,
        ): DemoApplicationServices =
            this.createBase(scope, serviceWorkDispatcher, enableSpeedLimit, physicalMediaData, onlineStoragesData, mountPoints, storagesOverride)
    }
}

fun String.toSlug() =
    lowercase()
        .replace("\n", " ")
        .replace("[^a-z\\d\\s]".toRegex(), "-")
        .replace("-+".toRegex(), "-")
        .trim('-')
