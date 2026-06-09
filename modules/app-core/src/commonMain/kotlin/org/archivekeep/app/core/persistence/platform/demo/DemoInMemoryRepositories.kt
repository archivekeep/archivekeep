package org.archivekeep.app.core.persistence.platform.demo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.archivekeep.app.core.api.repository.location.RepositoryLocationAccessor
import org.archivekeep.app.core.api.repository.location.RepositoryLocationContentsState
import org.archivekeep.app.core.domain.repositories.RepositoryConnectionState
import org.archivekeep.app.core.domain.repositories.RepositoryEncryptionType
import org.archivekeep.app.core.domain.repositories.RepositoryInformation
import org.archivekeep.app.core.domain.repositories.ResolvedRepositoryState
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.domain.storages.StorageNamedReference
import org.archivekeep.app.core.domain.storages.StorageRepository
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.persistence.registry.RegisteredStorage
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.generics.UniqueSharedFlowInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.api.repository.LocalRepo
import org.archivekeep.files.api.repository.PLAIN_REPOSITORY_TYPE
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepositoryAssociationGroupId
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.files.driver.speedlimit.SpeedLimitedLocalRepoWrapper
import org.archivekeep.files.driver.speedlimit.SpeedLimitedRepoWrapper
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.stateIn
import org.archivekeep.utils.loading.stateIn

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

private fun DemoRepository.inStorage(
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
        repo =
            repoFactory(
                contentsFixture,
                RepositoryMetadata(
                    repositoryType = PLAIN_REPOSITORY_TYPE,
                    associationGroupId = correlationId,
                ),
            ),
    )

@Inject
@SingleIn(AppScope::class)
class DemoInMemoryRepositories(
    private val scope: CoroutineScope,
    private val physicalMediaData: List<DemoPhysicalMedium>,
    private val onlineStoragesData: List<DemoOnlineStorage>,
    @param:Named("enableSpeedLimit") private val enableSpeedLimit: Boolean,
) : StorageDriver(DemoRepositoryURIData.ID),
    RegistryDataStore {
    data class InMemoryStorage(
        val registeredStorage: RegisteredStorage,
        val repos: Flow<List<MockedRepository>>,
    )

    private val liveStatusFlowManager =
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
                .mapValues { (_, value) ->
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

    override fun openLocation(uri: RepositoryURI): RepositoryLocationAccessor =
        object : RepositoryLocationAccessor {
            override val contentsStateFlow: Flow<OptionalLoadable<RepositoryLocationContentsState>> =
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
                        emit(
                            (
                                OptionalLoadable.LoadedAvailable(
                                    object :
                                        RepositoryLocationContentsState.IsRepositoryLocation {
                                        override val repoStateFlow: Flow<RepositoryAccessState> =
                                            flowOf(OptionalLoadable.LoadedAvailable(repo))
                                    },
                                )
                            ),
                        )
                    }
                }.stateIn(scope)
        }
}
