package org.archivekeep.app.core.domain.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.plus
import org.archivekeep.app.core.api.repository.location.RepositoryLocationAccessor
import org.archivekeep.app.core.api.repository.location.autoUnlocker
import org.archivekeep.app.core.api.repository.location.repositoryAccessor
import org.archivekeep.app.core.domain.storages.asStatus
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository.Companion.memorizingCachingIndexFlow
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository.Companion.memorizingCachingMetadataFlow
import org.archivekeep.app.core.utils.exceptions.RepositoryLockedException
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.exceptions.UnsupportedFeatureException
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.utils.coroutines.InstanceProtector
import org.archivekeep.utils.coroutines.shareResourceIn
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.firstFinished
import org.archivekeep.utils.loading.optional.flatMapLatestLoadedData
import org.archivekeep.utils.loading.optional.mapIfLoadedOrNull
import org.archivekeep.utils.loading.optional.mapLoaded
import org.archivekeep.utils.loading.optional.mapLoadedDataAsOptional
import org.archivekeep.utils.loading.optional.mapToLoadable
import org.archivekeep.utils.loading.optional.stateIn

private val InstanceProtector = InstanceProtector<Repository>()

/**
 * Object to access repository state:
 *
 * * what is this repository (ID and name),
 * * what is in this repository (metadata (can be cached), contents,...).
 */
class Repository(
    baseScope: CoroutineScope,
    val uri: RepositoryURI,
    registeredRepositoryFlow: Flow<RegisteredRepository?>,
    private val repositoryAccessorProvider: RepositoryLocationAccessor,
    memorizedRepositoryIndexRepository: MemorizedRepositoryIndexRepository,
    private val memorizedRepositoryMetadataRepository: MemorizedRepositoryMetadataRepository,
    credentialsStore: CredentialsStore,
) {
    private val scope = baseScope + InstanceProtector.forInstance(this)

    val optionalAccessorFlow =
        repositoryAccessorProvider
            .contentsStateFlow
            .repositoryAccessor()
            .autoUnlocker(uri, credentialsStore)
            .stateIn(scope)

    @Deprecated("Switch to optionalAccessorFlow and handle NotAvailable on consumer side")
    val accessorFlow =
        optionalAccessorFlow
            .map {
                it.mapToLoadable {
                    Loadable.Failed(RuntimeException("Not available", it.cause))
                }
            }

    val indexFlowWithCaching =
        memorizedRepositoryIndexRepository
            .memorizingCachingIndexFlow(
                uri,
                optionalAccessorFlow,
            ).stateIn(scope)

    val metadataFlowWithCaching =
        memorizedRepositoryMetadataRepository
            .memorizingCachingMetadataFlow(
                uri,
                optionalAccessorFlow,
            ).stateIn(scope)

    val informationFlow =
        combine(
            registeredRepositoryFlow,
            metadataFlowWithCaching,
        ) { registryRepo, metadata ->
            RepositoryInformation(
                associationId = metadata.mapIfLoadedOrNull { it.associationGroupId },
                displayName = registryRepo?.displayLabel ?: uri.data,
            )
        }.shareResourceIn(scope)

    val resolvedState =
        combine(
            informationFlow,
            optionalAccessorFlow
                .map { it.asStatus() }
                .onStart { emit(RepositoryConnectionState.Disconnected) },
        ) { info, connectionStatus ->
            ResolvedRepositoryState(uri, info, connectionStatus)
        }.shareResourceIn(scope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val localRepoStatus =
        optionalAccessorFlow
            .mapLoaded {
                if (it is LocalRepo) {
                    OptionalLoadable.LoadedAvailable(it)
                } else {
                    OptionalLoadable.NotAvailable(RuntimeException("not working repository: ${it.javaClass.simpleName}"))
                }
            }.flatMapLatestLoadedData { repo -> repo.localIndex.mapLoadedDataAsOptional { it } }
            .stateIn(scope)

    suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) {
        try {
            requireLoadedAccessor()
                .updateMetadata(transform)
        } catch (e: UnsupportedFeatureException) {
            println("Metadata update not supported by driver: $e")

            val metadata =
                transform(
                    memorizedRepositoryMetadataRepository
                        .repositoryCachedMetadataFlow(uri)
                        .firstFinished()
                        ?: RepositoryMetadata(),
                )

            println("Setting memorized metadata: $metadata")
            memorizedRepositoryMetadataRepository.updateRepositoryMemorizedMetadataIfDiffers(uri, metadata)
        }
    }

    private suspend fun requireLoadedAccessor() =
        repositoryAccessorProvider
            .contentsStateFlow
            .repositoryAccessor()
            .transform {
                when (it) {
                    is OptionalLoadable.Failed -> throw it.cause
                    is OptionalLoadable.LoadedAvailable -> emit(it.value)
                    OptionalLoadable.Loading -> {}
                    // TODO
                    is OptionalLoadable.NotAvailable -> throw RepositoryLockedException(uri)
                }
            }.first()
}
