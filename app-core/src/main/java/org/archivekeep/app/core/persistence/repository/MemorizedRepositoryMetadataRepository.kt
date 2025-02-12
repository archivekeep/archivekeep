package org.archivekeep.app.core.persistence.repository

import kotlinx.coroutines.flow.Flow
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.RepositoryMetadata

/**
 * This is a bit more than merely a cache.
 *
 * Proper functionality depends on being able to remember internals of not connected storages and currently unavailable repositories.
 */
interface MemorizedRepositoryMetadataRepository {
    fun repositoryCachedMetadataFlow(uri: RepositoryURI): Flow<OptionalLoadable<RepositoryMetadata>>

    suspend fun updateRepositoryMemorizedMetadataIfDiffers(
        uri: RepositoryURI,
        metadata: RepositoryMetadata?,
    )
}
