package org.archivekeep.app.core.persistence.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.flatMapLatestLoadedData
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.exceptions.UnsupportedFeatureException
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.loading.Loadable

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

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        fun MemorizedRepositoryMetadataRepository.memorizingCachingMetadataFlow(
            uri: RepositoryURI,
            optionalAccessorFlow: Flow<OptionalLoadable<Repo>>,
        ): Flow<OptionalLoadable<RepositoryMetadata>> {
            val memorizedMetadataFlow: Flow<OptionalLoadable<RepositoryMetadata>> =
                repositoryCachedMetadataFlow(uri)

            return optionalAccessorFlow
                .flatMapLatestLoadedData(
                    onNotAvailable = { memorizedMetadataFlow },
                ) {
                    it.metadataFlow
                        .conflate()
                        .flatMapLatest { metadataLoadable ->
                            when (metadataLoadable) {
                                is Loadable.Failed -> {
                                    if (metadataLoadable.throwable is UnsupportedFeatureException) {
                                        println("Unsupported metadata by $uri, working with memorized")
                                        memorizedMetadataFlow
                                    } else {
                                        flowOf(OptionalLoadable.Failed(metadataLoadable.throwable))
                                    }
                                }

                                Loadable.Loading -> flowOf(OptionalLoadable.Loading)
                                is Loadable.Loaded -> {
                                    updateRepositoryMemorizedMetadataIfDiffers(
                                        uri,
                                        metadataLoadable.value,
                                    )

                                    flowOf(OptionalLoadable.LoadedAvailable(metadataLoadable.value))
                                }
                            }
                        }
                }.onEach {
                    println("Loaded repository metadata for $uri - $it")
                }
        }
    }
}
