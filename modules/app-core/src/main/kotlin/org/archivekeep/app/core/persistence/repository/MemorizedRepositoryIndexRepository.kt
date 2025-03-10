package org.archivekeep.app.core.persistence.repository

import kotlinx.coroutines.flow.Flow
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.RepoIndex

/**
 * This is a bit more than merely a cache.
 *
 * Proper functionality depends on being able to remember internals of not connected storages and currently unavailable repositories.
 */
interface MemorizedRepositoryIndexRepository {
    fun repositoryMemorizedIndexFlow(uri: RepositoryURI): Flow<OptionalLoadable<RepoIndex>>

    suspend fun updateRepositoryMemorizedIndexIfDiffers(
        uri: RepositoryURI,
        accessedIndex: RepoIndex?,
    )
}
