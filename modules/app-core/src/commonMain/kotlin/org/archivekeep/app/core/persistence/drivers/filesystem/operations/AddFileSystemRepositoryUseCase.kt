package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType

interface AddFileSystemRepositoryUseCase {
    suspend fun begin(
        scope: CoroutineScope,
        path: String,
        intendedStorageType: FileSystemStorageType?,
    ): AddFileSystemRepositoryOperation
}
