package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope

interface DeinitializeFileSystemRepositoryUseCase {
    suspend fun prepare(
        scope: CoroutineScope,
        path: String,
    ): DeinitializeFileSystemRepositoryPreparation
}
