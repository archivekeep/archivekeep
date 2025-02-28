package org.archivekeep.app.core.persistence.drivers.filesystem

import org.archivekeep.app.core.utils.exceptions.DisconnectedStorageException

class FileSystemNotFoundException(
    fsUUID: String,
) : RuntimeException("FS not found: $fsUUID"),
    DisconnectedStorageException
