package org.archivekeep.app.core.persistence.drivers.filesystem

import android.content.Context
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onStart
import org.archivekeep.utils.io.debounceAndRepeatAfterDelay
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.ResourceLoader
import org.archivekeep.utils.loading.mapLoadedData
import java.util.Date
import java.util.concurrent.Executor

class AndroidFileStores(
    val context: Context,
    scope: CoroutineScope,
    executor: Executor,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FileStores {
    private val storageManager: StorageManager
        get() = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

    // TODO: trigger on permission grant
    private val changes = Channel<Date>(capacity = 1)

    private val autoRefreshMountPoints =
        ResourceLoader(
            scope,
            updateTriggerFlow =
                changes
                    .consumeAsFlow()
                    .onStart { emit(Date()) }
                    .debounceAndRepeatAfterDelay(),
            loadFn = {
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

                listOf(internal) + external
            },
            dispatcher = ioDispatcher,
        )

    override suspend fun loadFreshMountPoints() = autoRefreshMountPoints.getFreshAndUpdateState()

    override val mountPoints = autoRefreshMountPoints.stateFlow

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

    private val storageVolumeCallback =
        object : StorageManager.StorageVolumeCallback() {
            override fun onStateChanged(volume: StorageVolume) {
                changes.trySendBlocking(Date())
            }
        }

    init {
        storageManager.registerStorageVolumeCallback(executor, storageVolumeCallback)
    }
}
