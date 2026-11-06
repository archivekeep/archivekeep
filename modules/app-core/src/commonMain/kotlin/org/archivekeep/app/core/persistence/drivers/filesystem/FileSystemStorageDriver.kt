package org.archivekeep.app.core.persistence.drivers.filesystem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import org.archivekeep.app.core.api.repository.location.PasswordRequest
import org.archivekeep.app.core.api.repository.location.RepositoryLocationAccessor
import org.archivekeep.app.core.api.repository.location.RepositoryLocationContentsState
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.persistence.credentials.ContentEncryptionPassword
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.utils.generics.UniqueInstanceManager
import org.archivekeep.app.core.utils.generics.UniqueSharedFlowInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.driver.filesystem.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.driver.filesystem.files.FileSystemRepositoryFactory
import org.archivekeep.files.driver.filesystem.files.FilesystemWorkingRepository
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable.LoadedAvailable
import org.archivekeep.utils.loading.optional.OptionalLoadable.NotAvailable
import org.archivekeep.utils.loading.optional.autoCatch
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
    val providers =
        UniqueInstanceManager(
            factory = { key: Pair<RepositoryURI, FileSystemRepositoryURIData> ->
                Provider(key.first, key.second)
            },
            closeCallback = { it.close() },
        )

    init {
        fileStores.onFileSystemRemoved { fsUUID ->
            closeAffectedProvidersOnFileSystemRemoved(fsUUID)
        }
    }

    private fun closeAffectedProvidersOnFileSystemRemoved(fsUUID: String) {
        val affected = providers.instances.filter { it.uriDATA.fsUUID == fsUUID }
        for (provider in affected) {
            try {
                // TODO: here's responsibility for close sync / coroutines lifecycle is split to call-site and manager
                provider.closeSync()
                providers.invalidateSync(provider)
            } catch (e: Throwable) {
                println("Error closing provider for ${provider.uri}: ${e.message}")
            }
        }
    }

    val liveStatusFlowManager =
        UniqueSharedFlowInstanceManager(
            scope,
            factory = { key: StorageURI ->
                fileStores.mountedFileSystems
                    .mapLoadedData { connectedFSList ->
                        val connectedFS = connectedFSList.firstOrNull { it.fsUUID == key.data }

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

    override fun openLocation(uri: RepositoryURI): RepositoryLocationAccessor = providers[Pair(uri, uri.typedRepoURIData as FileSystemRepositoryURIData)]

    class FileSystemRepoLocation(
        repo: Repo,
    ) : RepositoryLocationContentsState.IsRepositoryLocation {
        override val repoStateFlow = flowOf(LoadedAvailable(repo))
    }

    class EncryptedFileSystemRepoLocation(
        val repositoryURI: RepositoryURI,
        val pathInFilesystem: Path,
        val credentialsStore: CredentialsStore,
        val scope: CoroutineScope,
    ) : RepositoryLocationContentsState.IsRepositoryLocation {
        val opened = MutableStateFlow<EncryptedFileSystemRepository?>(null)

        fun close() {
            opened.value = null
        }

        override val repoStateFlow: Flow<RepositoryAccessState> =
            opened.map { repo ->
                if (repo != null) {
                    LoadedAvailable(repo)
                } else {
                    val passwordRequest =
                        PasswordRequest { providedPassword, rememberPassword ->
                            opened.value = EncryptedFileSystemRepository.openAndUnlock(pathInFilesystem, providedPassword)

                            if (rememberPassword) {
                                credentialsStore.saveRepositorySecret(repositoryURI, ContentEncryptionPassword, JsonPrimitive(providedPassword))
                            }
                        }

                    NeedsUnlock(passwordRequest)
                }
            }
    }

    inner class Provider(
        val uri: RepositoryURI,
        val uriDATA: FileSystemRepositoryURIData,
    ) : RepositoryLocationAccessor {
        val currentPlainRepo = MutableStateFlow<FilesystemWorkingRepository?>(null)
        val currentEncryptedRepo = MutableStateFlow<EncryptedFileSystemRepoLocation?>(null)

        override suspend fun close() {
            closeCurrentRepositories()
        }

        fun closeSync() {
            currentPlainRepo.value?.closeSync()
            currentPlainRepo.value = null
            currentEncryptedRepo.value?.close()
            currentEncryptedRepo.value = null
        }

        override val contentsStateFlow: StateFlow<OptionalLoadable<RepositoryLocationContentsState>> =
            getPathInFileSystem(uriDATA)
                .distinctUntilChanged()
                .conflate()
                .transform { pathState ->
                    emit(OptionalLoadable.Loading)
                    closeCurrentRepositories()

                    emit(
                        when (pathState) {
                            is LoadedAvailable -> openRepository(pathState.value)
                            is NotAvailable -> pathState
                            is OptionalLoadable.Failed -> pathState
                            is OptionalLoadable.Loading -> OptionalLoadable.Loading
                        },
                    )
                }.autoCatch()
                .onCompletion {
                    withContext(NonCancellable) {
                        closeCurrentRepositories()
                    }
                }.stateIn(
                    scope,
                    SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0),
                    OptionalLoadable.Loading,
                )

        private suspend fun openRepository(pathInFilesystem: Path): OptionalLoadable<RepositoryLocationContentsState> {
            val plainRepo = FileSystemRepositoryFactory.openPlainRepositoryOrNull(pathInFilesystem)
            if (plainRepo != null) {
                currentPlainRepo.value = plainRepo
                return LoadedAvailable(FileSystemRepoLocation(plainRepo))
            }

            if (EncryptedFileSystemRepository.isRepository(pathInFilesystem)) {
                val encryptedLocation =
                    EncryptedFileSystemRepoLocation(
                        uri,
                        pathInFilesystem,
                        credentialsStore,
                        scope + SupervisorJob(),
                    )
                currentEncryptedRepo.value = encryptedLocation
                return LoadedAvailable(encryptedLocation)
            }

            return OptionalLoadable.Failed(RuntimeException("Path $uriDATA is not repository"))
        }

        private suspend fun closeCurrentRepositories() {
            currentPlainRepo.value?.close()
            currentPlainRepo.value = null
            currentEncryptedRepo.value?.close()
            currentEncryptedRepo.value = null
        }
    }

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
