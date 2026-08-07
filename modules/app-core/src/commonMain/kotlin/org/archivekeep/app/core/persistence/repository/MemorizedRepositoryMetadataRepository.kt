package org.archivekeep.app.core.persistence.repository

import kotlinx.coroutines.flow.Flow
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.utils.loading.optional.OptionalLoadable

/**
 * This is a bit more than merely a cache.
 *
 * Proper functionality depends on being able to remember internals of not connected storages and currently unavailable repositories.
 */
interface MemorizedRepositoryMetadataRepository {
    fun repositoryCachedMetadataFlow(uri: RepositoryURI): Flow<OptionalLoadable<RepositoryMetadata>>

    suspend fun updateRepositoryMemorizedMetadataIfDiffers(
        uri: RepositoryURI,
        metadata: RepositoryMetadata,
    )
}
