package org.archivekeep.app.core.persistence.drivers.filesystem

import org.archivekeep.app.core.utils.identifiers.StorageURI

data class MountedFileSystem(
    val fsUUID: String,
    val fsLabel: String,
    val mountPoints: List<MountPoint>,
) {
    data class MountPoint(
        val mountPath: String,
        val fsLabel: String,
        val fsUUID: String,
        val fsSubPath: String,
    ) {
        val storageURI by lazy { StorageURI(FileSystemRepositoryURIData.ID, fsUUID) }
    }
}
