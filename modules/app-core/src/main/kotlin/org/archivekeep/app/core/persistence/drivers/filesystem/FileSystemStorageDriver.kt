package org.archivekeep.app.core.persistence.drivers.filesystem

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.OptionalLoadable.LoadedAvailable
import org.archivekeep.app.core.utils.generics.OptionalLoadable.NotAvailable
import org.archivekeep.app.core.utils.generics.UniqueSharedFlowInstanceManager
import org.archivekeep.app.core.utils.generics.mapLoaded
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.generics.mapLoadedDataAsOptional
import org.archivekeep.app.core.utils.generics.mapToOptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.files.openFilesRepoOrNull
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.waitLoadedValue
import java.nio.file.Path
import kotlin.io.path.Path

class FileSystemStorageDriver(
    val scope: CoroutineScope,
    val fileStores: FileStores,
) : StorageDriver {
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
                    }.waitLoadedValue()
            },
        )

    override fun getStorageAccessor(storageURI: StorageURI): StorageConnection =
        StorageConnection(
            storageURI,
            fileStores.mountedFileSystems
                .mapLoadedDataAsOptional { connectedFSList ->
                    connectedFSList
                        .filter { it.fsUUID == storageURI.data }
                        .firstOrNull()
                        ?.let { connectedFS ->
                            StorageInformation.Partition(
                                physicalID = connectedFS.fsUUID,
                                driveType = StorageInformation.Partition.DriveType.Other,
                            )
                        }
                }.shareResourceIn(scope),
            liveStatusFlowManager[storageURI],
        )

    override fun openRepoFlow(uri: RepositoryURI): Flow<ProtectedLoadableResource<Repo, RepoAuthRequest>> {
        // TODO: map parallel, partial loadable - some filesystems might be slow or unresponsive

        // TODO: reuse opened

        val uriDATA = uri.typedRepoURIData as FileSystemRepositoryURIData

        return getPathInFileSystem(uriDATA)
            .distinctUntilChanged()
            .map {
                when (it) {
                    OptionalLoadable.Loading -> ProtectedLoadableResource.Loading
                    is OptionalLoadable.Failed -> ProtectedLoadableResource.Failed(it.cause)
                    is NotAvailable -> ProtectedLoadableResource.Failed(it.cause ?: RuntimeException("Not found path in file system"))
                    is LoadedAvailable -> {
                        println("Open $uriDATA")

                        val openedRepo = openFilesRepoOrNull(it.value)

                        if (openedRepo == null) {
                            ProtectedLoadableResource.Failed(RuntimeException("Not repo"))
                        } else {
                            ProtectedLoadableResource.Loaded(openedRepo)
                        }
                    }
                }
            }.onEach {
                println("New repo accessor for $uriDATA: $it")
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
