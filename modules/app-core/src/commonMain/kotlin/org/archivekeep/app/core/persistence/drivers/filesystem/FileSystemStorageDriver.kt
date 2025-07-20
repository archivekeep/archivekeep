package org.archivekeep.app.core.persistence.drivers.filesystem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.RepositoryAccessorProvider
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.persistence.credentials.ContentEncryptionPassword
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.utils.generics.UniqueSharedFlowInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.repo.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.repo.files.FilesRepo
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable.LoadedAvailable
import org.archivekeep.utils.loading.optional.OptionalLoadable.NotAvailable
import org.archivekeep.utils.loading.optional.flatMapLatestLoadedData
import org.archivekeep.utils.loading.optional.mapLoaded
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.loading.optional.mapLoadedDataAsOptional
import org.archivekeep.utils.loading.optional.mapToOptionalLoadable
import org.archivekeep.utils.loading.optional.stateIn
import java.nio.file.Path
import kotlin.io.path.Path

class FileSystemStorageDriver(
    val scope: CoroutineScope,
    val fileStores: FileStores,
    val credentialsStore: CredentialsStore,
) : StorageDriver(FileSystemRepositoryURIData.ID) {
    val liveStatusFlowManager =
        UniqueSharedFlowInstanceManager(
            scope,
            factory = { key: StorageURI ->
                fileStores.mountedFileSystems
                    .mapLoadedData { connectedFSList ->
                        val connectedFS =
                            connectedFSList
                                .filter { it.fsUUID == key.data }
                                .firstOrNull()

                        if (connectedFS != null) {
                            Storage.ConnectionStatus.CONNECTED
                        } else {
                            Storage.ConnectionStatus.DISCONNECTED
                        }
                    }.onEach {
                        println("Storage status: $key = $it")
                    }
            },
        )

    override fun getStorageAccessor(storageURI: StorageURI): StorageConnection =
        StorageConnection(
            storageURI,
            StorageInformation.Partition(
                details =
                    fileStores.mountedFileSystems
                        .mapLoadedDataAsOptional { connectedFSList ->
                            connectedFSList
                                .firstOrNull { it.fsUUID == storageURI.data }
                                ?.let { connectedFS ->
                                    StorageInformation.Partition.Details(
                                        physicalID = connectedFS.fsUUID,
                                        // TODO - implement real
                                        driveType = StorageInformation.Partition.DriveType.Other,
                                    )
                                }
                        }.stateIn(scope),
            ),
            liveStatusFlowManager[storageURI],
        )

    override fun getProvider(uri: RepositoryURI): RepositoryAccessorProvider = Provider(uri, uri.typedRepoURIData as FileSystemRepositoryURIData)

    private sealed interface InnerState {
        val repoStateFlow: Flow<RepositoryAccessState>

        class FileSystemRepo(
            val repo: FilesRepo,
        ) : InnerState {
            override val repoStateFlow = flowOf(LoadedAvailable(this.repo))
        }

        class EncryptedFileSystemRepo(
            val repositoryURI: RepositoryURI,
            val pathInFilesystem: Path,
            val credentialsStore: CredentialsStore,
            val scope: CoroutineScope,
        ) : InnerState {
            val opened = MutableStateFlow<EncryptedFileSystemRepository?>(null)

            val passwordRequest =
                PasswordRequest { providedPassword, rememberPassword ->
                    opened.value = EncryptedFileSystemRepository.openAndUnlock(pathInFilesystem, providedPassword)

                    if (rememberPassword) {
                        credentialsStore.saveRepositorySecret(repositoryURI, ContentEncryptionPassword, JsonPrimitive(providedPassword))
                    }
                }

            val autoOpenFlow =
                flow<Unit> {
                    credentialsStore
                        .getRepositorySecretsFlow(repositoryURI)
                        .map { optionalCredentials ->
                            println("Received credentials: $optionalCredentials")

                            when (optionalCredentials) {
                                OptionalLoadable.Loading -> false
                                is OptionalLoadable.Failed -> false
                                is LoadedAvailable -> {
                                    optionalCredentials.value[ContentEncryptionPassword]?.let { contentEncryptionPassword ->
                                        if (opened.value == null) {
                                            try {
                                                passwordRequest.providePassword(contentEncryptionPassword.jsonPrimitive.content, false)

                                                return@map true
                                            } catch (e: Throwable) {
                                                // TODO: expose failure to UI, or move auto-unlocking responsibility out of driver logic
                                                println("Auto-unlock failed: $e")
                                            }
                                        }
                                    }

                                    false
                                }
                                is NotAvailable -> false
                            }
                        }.takeWhile { success -> !success }
                        .collect {}
                }.shareIn(scope, SharingStarted.WhileSubscribed(0, 0))

            override val repoStateFlow: Flow<RepositoryAccessState> =
                channelFlow {
                    val autoOpenCollector = launch(start = CoroutineStart.LAZY) { autoOpenFlow.collect {} }

                    opened
                        .onStart { autoOpenCollector.start() }
                        .onCompletion { autoOpenCollector.cancel() }
                        .collect { repo ->
                            send(
                                if (repo != null) {
                                    LoadedAvailable(repo)
                                } else {
                                    NeedsUnlock(passwordRequest)
                                },
                            )
                        }
                }
        }

        class NotRepository(
            val cause: Exception,
        ) : InnerState {
            override val repoStateFlow = flowOf(OptionalLoadable.Failed(this.cause))
        }
    }

    inner class Provider(
        val uri: RepositoryURI,
        val uriDATA: FileSystemRepositoryURIData,
    ) : RepositoryAccessorProvider {
        private val internalStateFlow =
            getPathInFileSystem(uriDATA)
                .distinctUntilChanged()
                .mapLoadedData { pathInFilesystem ->
                    println("Open $uriDATA")

                    FilesRepo
                        .openOrNull(pathInFilesystem)
                        ?.let {
                            return@mapLoadedData InnerState.FileSystemRepo(it)
                        }

                    if (EncryptedFileSystemRepository.isRepository(pathInFilesystem)) {
                        return@mapLoadedData InnerState.EncryptedFileSystemRepo(
                            uri,
                            pathInFilesystem,
                            credentialsStore,
                            scope + SupervisorJob(),
                        )
                    }

                    InnerState.NotRepository(RuntimeException("Path $uriDATA is not repository"))
                }.stateIn(scope)

        @OptIn(ExperimentalCoroutinesApi::class)
        override val repositoryAccessor = internalStateFlow.flatMapLatestLoadedData { it.repoStateFlow }
    }

    data class PasswordRequest(
        val providePassword: suspend (password: String, rememberPassword: Boolean) -> Unit,
    )

    fun getPathInFileSystem(repo: FileSystemRepositoryURIData): Flow<OptionalLoadable<Path>> {
        val pathInFS = repo.pathInFS

        return fileStores
            .mountedFileSystems
            .mapToOptionalLoadable()
            .mapLoaded { mountedFileSystems ->
                mountedFileSystems
                    .firstOrNull { it.fsUUID == repo.fsUUID }
                    ?.let { LoadedAvailable(it) }
                    ?: NotAvailable(FileSystemNotFoundException(repo.fsUUID))
            }.mapLoaded { connectedFS ->
                connectedFS.mountPoints
                    .filter { pathInFS.startsWith(it.fsSubPath) }
                    .maxByOrNull { it.fsSubPath.length }
                    ?.let { LoadedAvailable(it) }
                    ?: NotAvailable(MountPointNotFoundForPathException(pathInFS, connectedFS))
            }.mapLoadedData { mp ->
                Path(mp.mountPath).resolve(
                    pathInFS.removePrefix(mp.fsSubPath).removePrefix("/"),
                )
            }
    }
}
