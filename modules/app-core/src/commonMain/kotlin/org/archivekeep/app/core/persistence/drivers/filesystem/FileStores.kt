package org.archivekeep.app.core.persistence.drivers.filesystem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.utils.loading.Loadable

interface FileStores {
    val mountPoints: StateFlow<Loadable<List<MountedFileSystem.MountPoint>>>

    val mountedFileSystems: Flow<Loadable<List<MountedFileSystem>>>

    suspend fun loadFreshMountPoints(): List<MountedFileSystem.MountPoint>
}

fun List<MountedFileSystem.MountPoint>.getFileSystemForPath(path: String): MountedFileSystem.MountPoint? =
    this
        .filter { path.startsWith(it.mountPath) }
        .maxByOrNull { it.mountPath }
