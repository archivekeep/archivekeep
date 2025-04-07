package org.archivekeep.app.core.persistence.drivers.filesystem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.firstLoadedOrFailure

interface FileStores {
    val mountPoints: SharedFlow<Loadable<List<MountedFileSystem.MountPoint>>>

    val mountedFileSystems: Flow<Loadable<List<MountedFileSystem>>>

    suspend fun getFileSystemForPath(path: String): MountedFileSystem.MountPoint? {
        val mp = mountPoints.firstLoadedOrFailure().toMutableList()

        return mp
            .filter { path.startsWith(it.mountPath) }
            .maxByOrNull { it.mountPath }
    }
}
