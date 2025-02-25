package org.archivekeep.app.core.persistence.drivers.filesystem

import io.github.irgaly.kfswatch.KfsDirectoryWatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import org.archivekeep.app.core.utils.generics.sharedGlobalLoadableWhileSubscribed
import org.archivekeep.utils.io.watch
import org.archivekeep.utils.loading.firstLoadedOrFailure
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
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class FileStores(
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val updateFrequencyOrRateLimit = 100.milliseconds

    // TODO: ideally immediate + delayed refresh
    val updateRefreshDelay = 250.milliseconds

    val mediaPath = Path("/run/media/")

    val mediaDirectories =
        mediaPath
            .watch()
            .onEach {
                println("Media change: $it")
            }.debounce(updateFrequencyOrRateLimit)
            .map {
                delay(updateRefreshDelay)

                "Update after media change"
            }.onStart {
                emit("Get on start")
            }.map {
                println("Loading list of media, reason: $it")

                mediaPath.toFile().listFiles()?.filter {
                    it.isDirectory
                } ?: emptyList()
            }.flowOn(Dispatchers.IO)

    val devChangesFlow =
        mediaDirectories
            .transformLatest { mediaDirectories ->
                println("New media directories: $mediaDirectories")

                val scope = CoroutineScope(coroutineContext)
                val watcher = KfsDirectoryWatcher(scope)

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
                            .onEach {
                                println("Change: $it")
                            }.debounce(updateFrequencyOrRateLimit)
                            .map {
                                delay(updateRefreshDelay)

                                "update"
                            },
                    )
                } finally {
                    watcher.close()
                }
            }.flowOn(ioDispatcher)
            .conflate()

    val mountPoints =
        flow {
            val systemInfo = SystemInfo()

            suspend fun send() {
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

                emit(mountPoints)
            }

            send()

            devChangesFlow
                .conflate()
                .collect {
                    send()
                }
        }.flowOn(Dispatchers.IO)
            .sharedGlobalLoadableWhileSubscribed()

    val mountedFileSystems =
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

    suspend fun getFileSystemForPath(path: String): MountedFileSystem.MountPoint? {
        val mp = mountPoints.firstLoadedOrFailure().toMutableList()

        return mp
            .filter { path.startsWith(it.mountPath) }
            .maxByOrNull { it.mountPath }
    }
}
