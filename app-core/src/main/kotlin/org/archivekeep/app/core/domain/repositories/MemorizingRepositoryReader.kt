package org.archivekeep.app.core.domain.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.mapToOptionalLoadable
import org.archivekeep.app.core.utils.generics.sharedWhileSubscribed
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.exceptions.UnsupportedFeatureException
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.loading.Loadable

class MemorizingRepositoryReader(
    val scope: CoroutineScope,
    val uri: RepositoryURI,
    val optionalAccessorFlow: SharedFlow<OptionalLoadable<Repo>>,
    val memorizedRepositoryIndexRepository: MemorizedRepositoryIndexRepository,
    val memorizedRepositoryMetadataRepository: MemorizedRepositoryMetadataRepository,
) {
    private fun <T> memorizedFallback(
        st: String,
        a: Flow<OptionalLoadable<T>>,
        b: (repo: Repo) -> Flow<OptionalLoadable<T>>,
    ): Flow<OptionalLoadable<T>> =
        optionalAccessorFlow
            .flatMapLatest { optionalAccessorLoadable ->
                println("Accessor stuff for $st: $uri -> $optionalAccessorLoadable")

                when (optionalAccessorLoadable) {
                    is OptionalLoadable.Failed -> {
                        optionalAccessorLoadable.cause.printStackTrace()
                        flowOf(OptionalLoadable.Failed(optionalAccessorLoadable.cause))
                    }
                    OptionalLoadable.Loading -> flowOf(OptionalLoadable.Loading)
                    is OptionalLoadable.NotAvailable -> a
                    is OptionalLoadable.LoadedAvailable -> b(optionalAccessorLoadable.value)
                }
            }

    val indexFlow =
        run {
            val memorizedIndexFlow = memorizedRepositoryIndexRepository.repositoryMemorizedIndexFlow(uri)

            memorizedFallback("index flow", memorizedIndexFlow) {
                it.observable.indexFlow
                    .conflate()
                    .onEach { accessedIndexLoadable ->
                        val accessedIndex = (accessedIndexLoadable as? Loadable.Loaded)?.value ?: return@onEach

                        try {
                            memorizedRepositoryIndexRepository.updateRepositoryMemorizedIndexIfDiffers(
                                uri,
                                accessedIndex,
                            )
                        } catch (e: Throwable) {
                            println("ERROR: memorized index update failed: $e")
                            e.printStackTrace()
                        }
                    }.mapToOptionalLoadable()
            }.onEach {
                println("Loaded repository index for $uri")
            }.sharedWhileSubscribed(scope)
        }

    val metadataFlow: Flow<OptionalLoadable<RepositoryMetadata>> =
        run {
            val memorizedMetadataFlow: Flow<OptionalLoadable<RepositoryMetadata>> =
                memorizedRepositoryMetadataRepository.repositoryCachedMetadataFlow(uri)

            memorizedFallback("metadata flow", memorizedMetadataFlow) {
                println("Metadata stuff for: $uri -> $it")

                it.observable.metadataFlow
                    .flatMapLatest { metadataLoadable ->
                        println("Metadata stuff for 2: $uri -> $metadataLoadable")

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
                                memorizedRepositoryMetadataRepository.updateRepositoryMemorizedMetadataIfDiffers(
                                    uri,
                                    metadataLoadable.value,
                                )

                                flowOf(OptionalLoadable.LoadedAvailable(metadataLoadable.value))
                            }
                        }
                    }
            }.onEach {
                println("Loaded repository metadata for $uri - $it")
            }.sharedWhileSubscribed(scope)
        }
}
