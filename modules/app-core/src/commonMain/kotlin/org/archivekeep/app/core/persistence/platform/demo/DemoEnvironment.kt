package org.archivekeep.app.core.persistence.platform.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.archivekeep.app.core.domain.repositories.RepositoryConnectionState
import org.archivekeep.app.core.domain.repositories.RepositoryEncryptionType
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.domain.repositories.ResolvedRepositoryState
import org.archivekeep.app.core.domain.storages.KnownStorage
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.RepositoryAccessorProvider
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.persistence.credentials.CredentialsInProtectedWalletDataStore
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.credentials.WalletPO
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationDiscoveryForAdd
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.MountedFileSystem
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.persistence.registry.RegisteredStorage
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.utils.generics.UniqueSharedFlowInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.RepositoryAssociationGroupId
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.testing.fixtures.FixtureRepo
import org.archivekeep.testing.fixtures.FixtureRepoBuilder
import org.archivekeep.testing.storage.InMemoryLocalRepo
import org.archivekeep.testing.storage.InMemoryRepo
import org.archivekeep.testing.storage.SpeedLimitedLocalRepoWrapper
import org.archivekeep.testing.storage.SpeedLimitedRepoWrapper
import org.archivekeep.utils.datastore.passwordprotected.PasswordProtectedJoseStorageInFile
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.stateIn
import org.archivekeep.utils.loading.stateIn

open class DemoEnvironment(
    scope: CoroutineScope,
    enableSpeedLimit: Boolean = true,
    physicalMediaData: List<DemoPhysicalMedium> = listOf(LaptopSSD, LaptopHDD, hddB, hddC),
    onlineStoragesData: List<DemoOnlineStorage> = emptyList(),
    mountPoints: List<MountedFileSystem.MountPoint> = emptyList(),
) : Environment {
    data class InMemoryStorage(
        val registeredStorage: RegisteredStorage,
        val repos: Flow<List<MockedRepository>>,
    )

    override val fileStores: FileStores =
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

    private val demoTempDirectory = kotlin.io.path.createTempDirectory("archivekeep-demo-env")

    override val walletDataStore: PasswordProtectedJoseStorageInFile<WalletPO> =
        PasswordProtectedJoseStorageInFile(
            demoTempDirectory.resolve("credentials.jwe"),
            Json.serializersModule.serializer(),
            defaultValueProducer = { WalletPO(emptySet()) },
        )

    override val credentialsStore: CredentialsStore = CredentialsInProtectedWalletDataStore(walletDataStore)

    val mediaMapped =
        MutableStateFlow(
            physicalMediaData
                .associate {
                    val repositories = it.repositories.map { r -> r.inStorage(it.reference) }

                    it.id to
                        Pair(
                            InMemoryStorage(
                                registeredStorage =
                                    RegisteredStorage(
                                        uri = it.uri,
                                        label = it.displayName,
                                        isLocal = it.isLocal,
                                    ),
                                repos = MutableStateFlow(repositories),
                            ),
                            repositories,
                        )
                },
        )

    val onlineMapped =
        onlineStoragesData
            .associate {
                val repositories = it.repositories.map { r -> r.inStorage(it.reference) }

                it.id to repositories
            }

    override val registry =
        object : RegistryDataStore {
            override val registeredRepositories: MutableStateFlow<Set<RegisteredRepository>> =
                MutableStateFlow(
                    mediaMapped
                        .value
                        .flatMap { (_, e) ->
                            e.second.map { repo ->
                                val a =
                                    RegisteredRepository(
                                        uri = repo.uri,
                                        label = repo.displayName,
                                    )

                                a
                            }
                        }.toSet() +
                        onlineMapped
                            .flatMap { (_, e) ->
                                e.map { repo ->
                                    val a =
                                        RegisteredRepository(
                                            uri = repo.uri,
                                            label = repo.displayName,
                                        )

                                    a
                                }
                            }.toSet(),
                )

            override val registeredStorages: SharedFlow<Loadable<Set<RegisteredStorage>>> =
                mediaMapped
                    .map {
                        it
                            .map { (_, pair) ->
                                val (storage, _) = pair

                                storage.registeredStorage
                            }.toSet() +
                            onlineStoragesData
                                .map {
                                    RegisteredStorage(
                                        uri = it.uri,
                                        label = it.displayName,
                                        isLocal = false,
                                    )
                                }.toSet()
                    }.mapToLoadable()
                    .stateIn(scope)

            override suspend fun updateRepositories(fn: (old: Set<RegisteredRepository>) -> Set<RegisteredRepository>) {
                registeredRepositories.update(fn)
            }

            override suspend fun updateStorage(
                uri: StorageURI,
                transform: (storage: RegisteredStorage) -> RegisteredStorage,
            ) {
                mediaMapped.update { old ->
                    old
                        .mapValues { (key, value) ->
                            if (value.first.registeredStorage.uri == uri) {
                                value.copy(
                                    first =
                                        value.first.copy(
                                            registeredStorage = transform(value.first.registeredStorage),
                                        ),
                                )
                            } else {
                                value
                            }
                        }
                }
            }
        }

    fun repo(uri: RepositoryURI): MockedRepository? {
        mediaMapped
            .value
            .flatMap { it.value.second }
            .firstOrNull {
                it.uri == uri
            }?.let {
                return@repo it
            }

        onlineMapped
            .flatMap { it.value }
            .firstOrNull {
                it.uri == uri
            }?.let {
                return@repo it
            }

        return null
    }

    override val repositoryIndexMemory =
        object : MemorizedRepositoryIndexRepository {
            override fun repositoryMemorizedIndexFlow(uri: RepositoryURI): Flow<OptionalLoadable<RepoIndex>> = flowOf(OptionalLoadable.NotAvailable())

            override suspend fun updateRepositoryMemorizedIndexIfDiffers(
                uri: RepositoryURI,
                metadata: RepoIndex?,
            ) {
                // TODO
            }
        }

    override val repositoryMetadataMemory =
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

    val liveStatusFlowManager =
        UniqueSharedFlowInstanceManager(
            scope,
            factory = { key: StorageURI ->
                val realStatus =
                    physicalMediaData
                        .firstOrNull {
                            it.id == key.data
                        }?.connectionStatus

                if (realStatus == null) {
                    println("Repository with id `$key` not found")
                }

                flowOf(
                    Loadable.Loaded(realStatus ?: Storage.ConnectionStatus.DISCONNECTED),
                )
            },
        )

    override val storageDrivers: List<StorageDriver> =
        listOf(
            object : StorageDriver(DemoRepositoryURIData.ID) {
                override fun getStorageAccessor(storageURI: StorageURI): StorageConnection =
                    StorageConnection(
                        storageURI,
                        run {
                            val physicalStorage =
                                physicalMediaData.firstOrNull {
                                    it.id == storageURI.data
                                }
                            if (physicalStorage != null) {
                                return@run StorageInformation.Partition(
                                    flowOf(
                                        OptionalLoadable.LoadedAvailable(
                                            StorageInformation.Partition.Details(
                                                physicalID = physicalStorage.physicalID,
                                                // TODO - implement real
                                                driveType = StorageInformation.Partition.DriveType.Other,
                                            ),
                                        ),
                                    ),
                                )
                            }

                            val onlineStorage =
                                onlineStoragesData.firstOrNull {
                                    it.id == storageURI.data
                                }
                            if (onlineStorage != null) {
                                return@run StorageInformation.OnlineStorage
                            }

                            return@run StorageInformation.Error(RuntimeException("Storage with $storageURI not found"))
                        },
                        liveStatusFlowManager[storageURI],
                    )

                override fun getProvider(uri: RepositoryURI): RepositoryAccessorProvider =
                    object : RepositoryAccessorProvider {
                        override val repositoryAccessor: Flow<RepositoryAccessState> =
                            flow {
                                val repo =
                                    repo(uri)?.repo?.let { base ->
                                        if (enableSpeedLimit) {
                                            when (base) {
                                                is LocalRepo -> SpeedLimitedLocalRepoWrapper(base)
                                                else -> SpeedLimitedRepoWrapper(base)
                                            }
                                        } else {
                                            base
                                        }
                                    }

                                if (repo == null) {
                                    emit(OptionalLoadable.Failed(RuntimeException("Not repo")))
                                } else {
                                    emit((OptionalLoadable.LoadedAvailable(repo)))
                                }
                            }.stateIn(scope)
                    }

                override suspend fun discoverRepository(
                    uri: RepositoryURI,
                    credentials: BasicAuthCredentials?,
                ): RepositoryLocationDiscoveryForAdd {
                    TODO("Not yet implemented")
                }
            },
        )

    data class DemoPhysicalMedium(
        // this will probably differ in Linux and Windows
        // udevadm info --query=all /dev/nvme0n1 | grep ID_SERIAL=
        val physicalID: String,
        val driveType: StorageInformation.Partition.DriveType,
        val displayName: String,
        val isLocal: Boolean,
        val connectionStatus: Storage.ConnectionStatus,
        val id: String = displayName.toSlug(),
        val repositories: List<DemoRepository>,
    ) {
        val uri: StorageURI
            get() = StorageURI(DemoRepositoryURIData.ID, id)

        val reference = StorageNamedReference(uri, displayName)
    }

    data class DemoOnlineStorage(
        val displayName: String,
        val connectionStatus: Storage.ConnectionStatus,
        val id: String = displayName.toSlug(),
        val repositories: List<DemoRepository>,
    ) {
        val uri: StorageURI
            get() = StorageURI(DemoRepositoryURIData.ID, id)

        val reference = StorageNamedReference(uri, displayName)
    }

    data class DemoRepository(
        val displayName: String,
        val id: String = "a-${displayName.toSlug()}",
        val correlationId: RepositoryAssociationGroupId? = "a-${displayName.toSlug()}",
        val physicalLocation: String = "unknown",
        // TODO: factory maybe?
        val contentsFixture: FixtureRepo = FixtureRepo(emptyMap()),
        val repoFactory: (fixture: FixtureRepo, metadata: RepositoryMetadata) -> Repo = { fixture, metadata ->
            InMemoryRepo(
                fixture.contents.mapValues { (_, v) -> v.toByteArray() },
                metadata = metadata,
            )
        },
    ) {
        fun inStorage(
            storage: StorageNamedReference,
            encryptionType: RepositoryEncryptionType = RepositoryEncryptionType.NONE,
        ): MockedRepository =
            MockedRepository(
                storageID = storage.uri.data,
                name = id,
                storage = storage,
                associationId = correlationId,
                displayName = displayName,
                encryptionType = encryptionType,
                physicalLocation = physicalLocation,
                repo = repoFactory(contentsFixture, RepositoryMetadata(correlationId)),
            )

        fun localInMemoryFactory(): DemoRepository =
            this.copy(
                repoFactory = { fixture, metadata ->
                    InMemoryLocalRepo(
                        initialContents = fixture.contents.mapValues { (_, v) -> v.toByteArray() },
                        initialUnindexedContents = fixture.uncommittedContents.mapValues { (_, v) -> v.toByteArray() },
                        initialMissingContents = fixture.missingContents.mapValues { (_, v) -> v.toByteArray() },
                        metadata = metadata,
                    )
                },
            )

        fun withContents(modifications: FixtureRepoBuilder.() -> Unit): DemoRepository =
            this.copy(
                contentsFixture = contentsFixture.derive(modifications),
            )

        fun withNewContents(modifications: FixtureRepoBuilder.() -> Unit): DemoRepository =
            this.copy(
                contentsFixture = FixtureRepoBuilder().also(modifications).build(),
            )
    }

    data class MockedRepository(
        val storageID: String,
        val name: String,
        val storage: StorageNamedReference,
        val associationId: RepositoryAssociationGroupId?,
        val displayName: String,
        val encryptionType: RepositoryEncryptionType,
        val physicalLocation: String,
        val repo: Repo,
    ) {
        val uri: RepositoryURI = DemoRepositoryURIData(storageID, name).toURI()

        val storageRepository: StorageRepository =
            StorageRepository(
                storage,
                uri,
                ResolvedRepositoryState(
                    uri,
                    RepositoryInformation(
                        associationId,
                        displayName,
                    ),
                    RepositoryConnectionState.Connected,
                ),
            )
    }
}

fun String.toSlug() =
    lowercase()
        .replace("\n", " ")
        .replace("[^a-z\\d\\s]".toRegex(), "-")
        .replace("-+".toRegex(), "-")
        .trim('-')

fun DemoEnvironment.DemoPhysicalMedium.asKnownStorage(): KnownStorage =
    KnownStorage(
        this.uri,
        null,
        this.repositories.map {
            it.inStorage(this.reference).asRegisteredRepository()
        },
    )

fun DemoEnvironment.DemoPhysicalMedium.asKnownRegisteredStorage(): KnownStorage =
    KnownStorage(
        this.uri,
        RegisteredStorage(
            this.uri,
            this.displayName,
        ),
        this.repositories.map {
            it.inStorage(this.reference).asRegisteredRepository()
        },
    )

fun DemoEnvironment.MockedRepository.asRegisteredRepository(): RegisteredRepository = RegisteredRepository(this.uri, this.displayName)
