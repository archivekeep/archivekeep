package org.archivekeep.app.core.persistence.drivers.filesystem

import io.github.irgaly.kfswatch.KfsDirectoryWatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import org.archivekeep.utils.io.debounceAndRepeatAfterDelay
import org.archivekeep.utils.io.listFilesFlow
import org.archivekeep.utils.loading.AutoRefreshLoadableFlow
import org.archivekeep.utils.loading.mapLoadedData
import oshi.SystemInfo
import oshi.software.os.OSFileStore
import oshi.software.os.linux.LinuxFileSystem
import oshi.util.GlobalConfig
import oshi.util.platform.linux.DevPath
import oshi.util.platform.linux.ProcPath
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlin.io.path.Path
import kotlin.io.path.exists

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopFileStores(
    scope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FileStores {
    private val mediaPath = Path("/run/media/")

    private val mediaDirectoriesFlow =
        if (mediaPath.exists()) {
            mediaPath
                .listFilesFlow { it.isDirectory }
                .distinctUntilChanged()
        } else {
            flowOf(emptyList())
        }

    private val changeEventsFlow =
        mediaDirectoriesFlow
            .transformLatest { mediaDirectories ->
                val watcherScope = CoroutineScope(coroutineContext)
                val watcher = KfsDirectoryWatcher(watcherScope)

                try {
                    watcher.add(DevPath.MAPPER)
                    watcher.add(DevPath.DISK_BY_UUID)
                    watcher.add(ProcPath.MOUNTS)
                    watcher.add("/proc/self/mountinfo")
                    watcher.add("/run/media/")
                    watcher.add(*mediaDirectories.map { it.path }.toTypedArray())

                    emitAll(
                        watcher
                            .onEventFlow
                            .map { "update: $it" }
                            .onStart { emit("new media directories: $mediaDirectories") },
                    )
                } finally {
                    watcher.close()
                }
            }.catch {
                throw RuntimeException("watch dev changes error: ${it.message}", it)
            }.flowOn(ioDispatcher)
            .conflate()

    val autoRefreshMountPoints =
        AutoRefreshLoadableFlow(
            scope,
            ioDispatcher,
            loadFn = {
                val systemInfo = SystemInfo()

                GlobalConfig.set(LinuxFileSystem.OSHI_LINUX_FS_PATH_INCLUDES, "/run/media/**")

                val fileStores: List<OSFileStore> = systemInfo.operatingSystem.fileSystem.getFileStores(false)

                fileStores
                    .forEach { fs ->
                        println("mount: " + fs.mount)
                        println("name: " + fs.name)
                        println("type: " + fs.type)
                        println("description: " + fs.description)
                        println("uuid: " + fs.uuid)
                        println("str: $fs")
                        println("...")
                    }

                val mountinfo =
                    File("/proc/self/mountinfo")
                        .readLines()
                        .map { line ->
                            line.split(" ")
                        }

                val mountSubPath =
                    mountinfo.associate { line ->
                        line[4] to line[3]
                    }

                val mountPoints =
                    fileStores
                        .map { fs ->
                            MountedFileSystem.MountPoint(
                                mountPath = fs.mount,
                                fsLabel = fs.label,
                                fsUUID = fs.uuid,
                                fsSubPath = mountSubPath.getOrDefault(fs.mount, "/"),
                            )
                        }.filter {
                            // TODO: use more complex location identifier (primary UUID, fallback to other methods)
                            it.fsUUID.isNotBlank()
                        }.onEach {
                            println("mountPath: " + it.mountPath)
                            println("fsUUID: " + it.fsUUID)
                            println("fsSubPath: " + it.fsSubPath)
                            println("...")
                        }

                return@AutoRefreshLoadableFlow mountPoints
            },
            updateTriggerFlow =
                changeEventsFlow
                    .map { "change event: $it" }
                    .onStart { emit("start") }
                    .debounceAndRepeatAfterDelay(
                        mapDelayed = { "double-check retry after delay: $it" },
                    ).onEach { println("Collection of mounts triggered by: $it") },
        )

    override suspend fun loadFreshMountPoints() = autoRefreshMountPoints.getFreshAndUpdateState()

    override val mountPoints = autoRefreshMountPoints.stateFlow

    override val mountedFileSystems =
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
