package org.archivekeep.app.core.persistence.drivers.filesystem

import android.content.Context
import android.os.storage.StorageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapLoadedData
import org.archivekeep.utils.loading.mapToLoadable

class AndroidFileStores(
    context: Context,
    scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FileStores {
    override val mountPoints: SharedFlow<Loadable<List<MountedFileSystem.MountPoint>>> =
        flow {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

            val internal =
                run {
                    val directory = storageManager.primaryStorageVolume.directory!!

                    MountedFileSystem.MountPoint(directory.absolutePath, "Primary", "primary", "")
                }

            val external =
                storageManager.storageVolumes.mapNotNull {
                    val directory = it.directory
                    val uuid = it.uuid

                    if (directory == null || uuid == null) {
                        null
                    } else {
                        MountedFileSystem.MountPoint(directory.absolutePath, uuid, uuid, "")
                    }
                }

            emit(listOf(internal) + external)
        }.mapToLoadable()
            .flowOn(ioDispatcher)
            .shareResourceIn(scope, SharingStarted.Eagerly)

    override val mountedFileSystems: Flow<Loadable<List<MountedFileSystem>>> =
        mountPoints.mapLoadedData { mountPoints ->
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
}
