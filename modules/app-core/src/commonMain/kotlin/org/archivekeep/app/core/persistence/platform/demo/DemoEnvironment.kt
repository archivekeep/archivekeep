package org.archivekeep.app.core.persistence.platform.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.repositories.Repository
import org.archivekeep.app.core.domain.repositories.RepositoryEncryptionType
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.domain.repositories.ResolvedRepositoryState
import org.archivekeep.app.core.domain.storages.KnownStorage
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.persistence.credentials.Credentials
import org.archivekeep.app.core.persistence.credentials.JoseStorage
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.MountedFileSystem
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.persistence.registry.RegisteredStorage
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.UniqueSharedFlowInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.RepositoryAssociationGroupId
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.testing.fixtures.FixtureRepo
import org.archivekeep.testing.fixtures.FixtureRepoBuilder
import org.archivekeep.testing.storage.InMemoryLocalRepo
import org.archivekeep.testing.storage.InMemoryRepo
import org.archivekeep.testing.storage.SpeedLimitedLocalRepoWrapper
import org.archivekeep.testing.storage.SpeedLimitedRepoWrapper
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable

class DemoEnvironment(
    scope: CoroutineScope,
    enableSpeedLimit: Boolean = true,
    physicalMediaData: List<DemoPhysicalMedium> = listOf(LaptopSSD, LaptopHDD, hddB, hddC),
    onlineStoragesData: List<DemoOnlineStorage> = emptyList(),
) : Environment {
    data class InMemoryStorage(
        val registeredStorage: RegisteredStorage,
        val repos: Flow<List<MockedRepository>>,
    )

    override val fileStores: FileStores =
        object : FileStores {
            override val mountPoints: SharedFlow<Loadable<List<MountedFileSystem.MountPoint>>> =
                flowOf(emptyList<MountedFileSystem.MountPoint>())
                    .mapToLoadable()
                    .shareResourceIn(scope)

            override val mountedFileSystems: Flow<Loadable<List<MountedFileSystem>>> = flowOf(Loadable.Loaded(emptyList()))

            override suspend fun getFileSystemForPath(path: String): MountedFileSystem.MountPoint? = null
        }

    private val demoTempDirectory = kotlin.io.path.createTempDirectory("archivekeep-demo-env")

    override val walletDataStore: JoseStorage<Credentials> =
        JoseStorage(
            demoTempDirectory.resolve("credentials.jwe"),
            Json.serializersModule.serializer(),
            defaultValueProducer = { Credentials(emptySet()) },
        )

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
                                        isLocal = it.logicalLocation.startsWith("localhost"),
                                    ),
                                repos = MutableStateFlow(repositories),
                            ),
                            repositories,
                        )
                },
        )

    override val registry =
        object : RegistryDataStore {
            override val registeredRepositories: SharedFlow<Set<RegisteredRepository>> =
                mediaMapped
                    .map {
                        it
                            .flatMap { (_, e) ->
                                e.second.map { repo ->
                                    val a =
                                        RegisteredRepository(
                                            uri = repo.uri,
                                            label = repo.displayName,
                                        )

                                    println("Created: $a -> ${a.uri.typedRepoURIData.storageURI}")

                                    a
                                }
                            }.toSet()
                    }.shareResourceIn(scope)

            override val registeredStorages: SharedFlow<Loadable<Set<RegisteredStorage>>> =
                mediaMapped
                    .map {
                        it
                            .map { (_, pair) ->
                                val (storage, _) = pair

                                storage.registeredStorage
                            }.toSet()
                    }.mapToLoadable()
                    .shareResourceIn(scope)

            override suspend fun updateRepositories(fn: (old: Set<RegisteredRepository>) -> Set<RegisteredRepository>) {
                TODO("Not yet implemented")
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

    val onlineMapped =
        onlineStoragesData
            .associate {
                val repositories = it.repositories.map { r -> r.inStorage(it.reference) }

                it.id to repositories
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

    override val storageDrivers: Map<String, StorageDriver> =
        mapOf(
            "demo" to
                object : StorageDriver {
                    override fun getStorageAccessor(storageURI: StorageURI): StorageConnection =
                        StorageConnection(
                            storageURI,
                            flowOf(OptionalLoadable.NotAvailable()),
                            liveStatusFlowManager[storageURI],
                        )

                    override fun openRepoFlow(uri: RepositoryURI): Flow<ProtectedLoadableResource<Repo, RepoAuthRequest>> =
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
                                emit(ProtectedLoadableResource.Failed(RuntimeException("Not repo")))
                            } else {
                                emit((ProtectedLoadableResource.Loaded(repo)))
                            }
                        }
                },
        )

    data class DemoPhysicalMedium(
        // this will probably differ in Linux and Windows
        // udevadm info --query=all /dev/nvme0n1 | grep ID_SERIAL=
        val physicalID: String,
        val driveType: StorageInformation.Partition.DriveType,
        val displayName: String,
        val logicalLocation: String,
        val connectionStatus: Storage.ConnectionStatus,
        val id: String = displayName.toSlug(),
        val repositories: List<DemoRepository>,
    ) {
        val uri: StorageURI
            get() =
                StorageURI(
                    "demo",
                    id,
                )

        val reference = StorageNamedReference(uri, displayName)
    }

    data class DemoOnlineStorage(
        val displayName: String,
        val connectionStatus: Storage.ConnectionStatus,
        val id: String = displayName.toSlug(),
        val repositories: List<DemoRepository>,
    ) {
        val uri: StorageURI
            get() =
                StorageURI(
                    "demo",
                    id,
                )

        val reference = StorageNamedReference(uri, displayName)
    }

    data class DemoRepository(
        val displayName: String,
        val correlationId: RepositoryAssociationGroupId = "a-${displayName.toSlug()}",
        val physicalLocation: String = "unknown",
        // TODO: factory maybe?
        val contentsFixture: FixtureRepo = FixtureRepo(emptyMap()),
        val repoFactory: (fixture: FixtureRepo) -> Repo = { fixture ->
            InMemoryRepo(
                fixture.contents.mapValues { (_, v) -> v.toByteArray() },
                metadata =
                    RepositoryMetadata(
                        "a-${displayName.toSlug()}",
                    ),
            )
        },
    ) {
        fun inStorage(
            storage: StorageNamedReference,
            encryptionType: RepositoryEncryptionType = RepositoryEncryptionType.NONE,
        ): MockedRepository =
            MockedRepository(
                storageID = storage.uri.data,
                name = correlationId,
                storage = storage,
                associationId = correlationId,
                displayName = displayName,
                encryptionType = encryptionType,
                physicalLocation = physicalLocation,
                repo = repoFactory(contentsFixture),
            )

        fun localInMemoryFactory(): DemoRepository =
            this.copy(
                repoFactory = { fixture ->
                    InMemoryLocalRepo(
                        initialContents = fixture.contents.mapValues { (_, v) -> v.toByteArray() },
                        initialUnindexedContents = fixture.uncommittedContents.mapValues { (_, v) -> v.toByteArray() },
                        metadata =
                            RepositoryMetadata(
                                "a-${displayName.toSlug()}",
                            ),
                    )
                },
            )

        fun withContents(modifications: FixtureRepoBuilder.() -> Unit): DemoRepository =
            this.copy(
                contentsFixture = contentsFixture.derive(modifications),
            )
    }

    data class MockedRepository(
        val storageID: String,
        val name: String,
        val storage: StorageNamedReference,
        val associationId: RepositoryAssociationGroupId,
        val displayName: String,
        val encryptionType: RepositoryEncryptionType,
        val physicalLocation: String,
        val repo: Repo,
    ) {
        val uri: RepositoryURI =
            RepositoryURI(
                "demo",
                DemoRepositoryURIData(storageID, name).serialized(),
            )
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
                    Repository.ConnectionState.Connected,
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
