package org.archivekeep.app.core.persistence.drivers.filesystem.operations.deinitialize

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.utils.identifiers.RepositoryURI

interface DeinitializeFileSystemRepositoryByPathUseCase {
    suspend fun prepare(
        scope: CoroutineScope,
        path: String,
    ): DeinitializeFileSystemRepositoryPreparation
}

interface DeinitializeFileSystemRepositoryByUriUseCase {
    suspend fun prepare(
        scope: CoroutineScope,
        repositoryURI: RepositoryURI,
    ): DeinitializeFileSystemRepositoryPreparation
}
