package org.archivekeep.app.core.persistence.drivers.filesystem

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.exceptions.DisconnectedStorageException
import org.archivekeep.app.core.utils.generics.UniqueSharedFlowInstanceManager
import org.archivekeep.app.core.utils.generics.mapLoadedData
import org.archivekeep.app.core.utils.generics.mapLoadedDataToOptional
import org.archivekeep.app.core.utils.generics.sharedGlobalWhileSubscribed
import org.archivekeep.app.core.utils.generics.waitLoadedValue
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.files.openFilesRepoOrNull
import org.archivekeep.utils.Loadable
import kotlin.io.path.Path

class FileSystemStorageDriver(
    val fileStores: FileStores,
) : StorageDriver {
    val liveStatusFlowManager =
        UniqueSharedFlowInstanceManager(
            GlobalScope,
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
                .mapLoadedDataToOptional { connectedFSList ->
                    connectedFSList
                        .filter { it.fsUUID == storageURI.data }
                        .firstOrNull()
                        ?.let { connectedFS ->
                            StorageInformation.Partition(
                                physicalID = connectedFS.fsUUID,
                                driveType = StorageInformation.Partition.DriveType.Other,
                            )
                        }
                }.sharedGlobalWhileSubscribed(),
            liveStatusFlowManager[storageURI],
        )

    override fun openRepoFlow(uri: RepositoryURI): Flow<ProtectedLoadableResource<Repo, RepoAuthRequest>> {
        // TODO: map parallel, partial loadable - some filesystems might be slow or unresponsive

        // TODO: reuse opened

        val uriDATA = uri.typedRepoURIData as FileSystemRepositoryURIData

        return fileStores
            .mountedFileSystems
            .transform {
                when (it) {
                    is Loadable.Failed -> throw it.throwable
                    is Loadable.Loaded -> emit(it.value.firstOrNull { it.fsUUID == uriDATA.fsUUID })
                    Loadable.Loading -> {}
                }
            }.distinctUntilChanged()
            .transform { connectedFS ->
                accessor(uriDATA, connectedFS)
            }.onEach {
                println("New repo accessor for $uriDATA: $it")
            }
    }

    private suspend fun FlowCollector<ProtectedLoadableResource<Repo, RepoAuthRequest>>.accessor(
        repo: FileSystemRepositoryURIData,
        connectedFS: MountedFileSystem?,
    ) {
        val pathInFS = repo.pathInFS

        println("Open $repo in $connectedFS")

        if (connectedFS == null) {
            println("ERROR: FS not found: ${repo.fsUUID}")
            emit(
                ProtectedLoadableResource.Failed(
                    object :
                        RuntimeException("ERROR: FS not found: ${repo.fsUUID}"),
                        DisconnectedStorageException {},
                ),
            )
            return
        }

        val mp =
            connectedFS.mountPoints
                .filter { pathInFS.startsWith(it.fsSubPath) }
                .maxByOrNull { it.fsSubPath.length }

        if (mp == null) {
            println("ERROR: mount point not found: $pathInFS in ${connectedFS.mountPoints}")
            emit(
                ProtectedLoadableResource.Failed(
                    object :
                        RuntimeException("ERROR: mount point not found: $pathInFS in ${connectedFS.mountPoints}"),
                        DisconnectedStorageException {},
                ),
            )
            return
        }

        val p =
            Path(mp.mountPath).resolve(
                pathInFS.removePrefix(mp.fsSubPath).removePrefix("/"),
            )

        val openedRepo = openFilesRepoOrNull(p)

        if (openedRepo == null) {
            emit(ProtectedLoadableResource.Failed(RuntimeException("Not repo")))
        } else {
            emit((ProtectedLoadableResource.Loaded(openedRepo)))
        }
    }
}
