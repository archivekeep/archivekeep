package org.archivekeep.app.core.persistence.drivers.filesystem

import org.archivekeep.app.core.utils.exceptions.DisconnectedStorageException

class MountPointNotFoundForPathException(
    pathInFS: String,
    connectedFS: MountedFileSystem,
) : RuntimeException("Mount point not found: $pathInFS in ${connectedFS.mountPoints}"),
    DisconnectedStorageException
