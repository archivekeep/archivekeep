package org.archivekeep.app.core.persistence.drivers.filesystem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
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
import org.archivekeep.app.core.utils.generics.UniqueSharedFlowInstanceManager
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.driver.filesystem.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.driver.filesystem.files.FilesRepo
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable.LoadedAvailable
import org.archivekeep.utils.loading.optional.OptionalLoadable.NotAvailable
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

    override fun openLocation(uri: RepositoryURI): RepositoryLocationAccessor = Provider(uri, uri.typedRepoURIData as FileSystemRepositoryURIData)

    class FileSystemRepoLocation(
        val repo: FilesRepo,
    ) : RepositoryLocationContentsState.IsRepositoryLocation {
        override val repoStateFlow = flowOf(LoadedAvailable(this.repo))
    }

    class EncryptedFileSystemRepoLocation(
        val repositoryURI: RepositoryURI,
        val pathInFilesystem: Path,
        val credentialsStore: CredentialsStore,
        val scope: CoroutineScope,
    ) : RepositoryLocationContentsState.IsRepositoryLocation {
        val opened = MutableStateFlow<EncryptedFileSystemRepository?>(null)

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
        override val contentsStateFlow: Flow<OptionalLoadable<RepositoryLocationContentsState>> =
            getPathInFileSystem(uriDATA)
                .distinctUntilChanged()
                .mapLoaded { pathInFilesystem ->
                    println("Open $uriDATA")

                    FilesRepo
                        .openOrNull(pathInFilesystem)
                        ?.let {
                            return@mapLoaded LoadedAvailable(FileSystemRepoLocation(it))
                        }

                    if (EncryptedFileSystemRepository.isRepository(pathInFilesystem)) {
                        return@mapLoaded LoadedAvailable(
                            EncryptedFileSystemRepoLocation(
                                uri,
                                pathInFilesystem,
                                credentialsStore,
                                scope + SupervisorJob(),
                            ),
                        )
                    }

                    return@mapLoaded OptionalLoadable.Failed(RuntimeException("Path $uriDATA is not repository"))
                }.stateIn(scope)
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
