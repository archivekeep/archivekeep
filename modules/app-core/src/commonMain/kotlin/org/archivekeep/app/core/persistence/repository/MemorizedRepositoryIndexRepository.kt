package org.archivekeep.app.core.persistence.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.flatMapLatestLoadedData
import org.archivekeep.utils.loading.optional.mapToOptionalLoadable

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

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        fun MemorizedRepositoryIndexRepository.memorizingCachingIndexFlow(
            uri: RepositoryURI,
            optionalAccessorFlow: Flow<OptionalLoadable<Repo>>,
        ): Flow<OptionalLoadable<RepoIndex>> {
            val memorizedIndexFlow = repositoryMemorizedIndexFlow(uri)

            return optionalAccessorFlow
                .flatMapLatestLoadedData(
                    onNotAvailable = { memorizedIndexFlow },
                ) {
                    it.indexFlow
                        .onEach { accessedIndexLoadable ->
                            val accessedIndex = (accessedIndexLoadable as? Loadable.Loaded)?.value ?: return@onEach

                            try {
                                updateRepositoryMemorizedIndexIfDiffers(
                                    uri,
                                    accessedIndex,
                                )
                            } catch (e: Throwable) {
                                println("ERROR: memorized index update failed: $e")
                                e.printStackTrace()
                            }
                        }.mapToOptionalLoadable()
                }
        }
    }
}
