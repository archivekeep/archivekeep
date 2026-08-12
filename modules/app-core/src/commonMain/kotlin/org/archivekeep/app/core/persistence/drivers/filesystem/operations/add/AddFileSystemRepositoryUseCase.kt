package org.archivekeep.app.core.persistence.drivers.filesystem.operations.add

import kotlinx.coroutines.CoroutineScope

interface AddFileSystemRepositoryUseCase {
    suspend fun create(
        scope: CoroutineScope,
        path: String,
    ): AddFileSystemRepositoryOperation
}
