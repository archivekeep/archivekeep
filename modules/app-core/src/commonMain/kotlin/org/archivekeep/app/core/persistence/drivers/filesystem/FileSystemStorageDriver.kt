package org.archivekeep.app.core.persistence.drivers.filesystem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.RepositoryAccessorProvider
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.OptionalLoadable.LoadedAvailable
import org.archivekeep.app.core.utils.generics.OptionalLoadable.NotAvailable
import org.archivekeep.app.core.utils.generics.UniqueSharedFlowInstanceManager
import org.archivekeep.app.core.utils.generics.flatMapLatestLoadedData
import org.archivekeep.app.core.utils.generics.mapLoaded
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.generics.mapLoadedDataAsOptional
import org.archivekeep.app.core.utils.generics.mapToOptionalLoadable
import org.archivekeep.app.core.utils.generics.stateIn
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.repo.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.repo.files.FilesRepo
import org.archivekeep.utils.loading.mapLoadedData
import java.nio.file.Path
import kotlin.io.path.Path

class FileSystemStorageDriver(
    val scope: CoroutineScope,
    val fileStores: FileStores,
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
                                .filter { it.fsUUID == storageURI.data }
                                .firstOrNull()
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
        class FileSystemRepo(
            val repo: FilesRepo,
        ) : InnerState

        class EncryptedFileSystemRepo(
            val pathInFilesystem: Path,
        ) : InnerState {
            val opened = MutableStateFlow<EncryptedFileSystemRepository?>(null)

            val passwordRequest =
                PasswordRequest { providedPassword ->
                    opened.value = EncryptedFileSystemRepository.openAndUnlock(pathInFilesystem, providedPassword)
                }
        }

        class NotRepository(
            val cause: Exception,
        ) : InnerState
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
                        return@mapLoadedData InnerState.EncryptedFileSystemRepo(pathInFilesystem)
                    }

                    InnerState.NotRepository(RuntimeException("Path $uriDATA is not repository"))
                }.stateIn(scope)

        override val repositoryAccessor: Flow<RepositoryAccessState> =
            internalStateFlow
                .flatMapLatestLoadedData { innerState ->
                    when (innerState) {
                        is InnerState.EncryptedFileSystemRepo -> {
                            innerState
                                .opened
                                .map { repo ->
                                    if (repo != null) {
                                        LoadedAvailable(repo)
                                    } else {
                                        NeedsUnlock(innerState.passwordRequest)
                                    }
                                }
                        }

                        is InnerState.FileSystemRepo -> flowOf(LoadedAvailable(innerState.repo))
                        is InnerState.NotRepository -> flowOf(OptionalLoadable.Failed(innerState.cause))
                    }
                }
    }

    data class PasswordRequest(
        val providePassword: suspend (password: String) -> Unit,
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
