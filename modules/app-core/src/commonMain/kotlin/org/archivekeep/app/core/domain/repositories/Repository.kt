package org.archivekeep.app.core.domain.repositories

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.plus
import org.archivekeep.app.core.persistence.registry.RegisteredRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryIndexRepository
import org.archivekeep.app.core.persistence.repository.MemorizedRepositoryMetadataRepository
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.exceptions.DisconnectedStorageException
import org.archivekeep.app.core.utils.exceptions.RepositoryLockedException
import org.archivekeep.app.core.utils.generics.OptionalLoadable
import org.archivekeep.app.core.utils.generics.firstFinished
import org.archivekeep.app.core.utils.generics.flatMapLatestLoadedData
import org.archivekeep.app.core.utils.generics.mapIfLoadedOrNull
import org.archivekeep.app.core.utils.generics.mapLoaded
import org.archivekeep.app.core.utils.generics.mapToOptionalLoadable
import org.archivekeep.app.core.utils.generics.stateIn
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.mapAsLoadable
import org.archivekeep.files.exceptions.UnsupportedFeatureException
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepositoryMetadata
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.coroutines.InstanceProtector
import org.archivekeep.utils.coroutines.shareResourceIn

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
    val registeredRepositoryFlow: Flow<RegisteredRepository?>,
    val rawAccessor: Flow<ProtectedLoadableResource<Repo, RepoAuthRequest>>,
    val memorizedRepositoryIndexRepository: MemorizedRepositoryIndexRepository,
    val memorizedRepositoryMetadataRepository: MemorizedRepositoryMetadataRepository,
) {
    sealed class ConnectionState(
        val isAvailable: Boolean,
        val isConnected: Boolean,
        val isLocked: Boolean,
    ) {
        data object Disconnected : ConnectionState(false, false, false)

        data object ConnectedLocked : ConnectionState(true, false, true)

        data object Connected : ConnectionState(true, true, false)

        class Error(
            cause: Throwable?,
        ) : ConnectionState(false, false, false)
    }

    private val scope = baseScope + InstanceProtector.forInstance(this)

    val needsUnlock =
        rawAccessor
            .map { it is ProtectedLoadableResource.PendingAuthentication }
            .shareResourceIn(scope)

    val optionalAccessorFlow =
        rawAccessor
            .map {
                when (it) {
                    is ProtectedLoadableResource.Failed -> {
                        if (it.throwable is DisconnectedStorageException) {
                            OptionalLoadable.NotAvailable(it.throwable)
                        } else {
                            OptionalLoadable.Failed(it.throwable)
                        }
                    }
                    is ProtectedLoadableResource.Loaded -> OptionalLoadable.LoadedAvailable(it.value)
                    ProtectedLoadableResource.Loading -> OptionalLoadable.Loading
                    is ProtectedLoadableResource.PendingAuthentication -> OptionalLoadable.NotAvailable()
                }
            }.stateIn(scope)

    val connectionStatusFlow =
        rawAccessor
            .map {
                when (it) {
                    is ProtectedLoadableResource.Failed -> {
                        if (it.throwable is DisconnectedStorageException) {
                            ConnectionState.Disconnected
                        } else {
                            ConnectionState.Error(it.throwable)
                        }
                    }
                    is ProtectedLoadableResource.Loaded -> ConnectionState.Connected
                    ProtectedLoadableResource.Loading -> ConnectionState.Disconnected
                    is ProtectedLoadableResource.PendingAuthentication -> ConnectionState.ConnectedLocked
                }
            }.stateIn(scope, SharingStarted.WhileSubscribed(), ConnectionState.Disconnected)

    val memorizingRepositoryReader =
        MemorizingRepositoryReader(
            scope,
            uri,
            optionalAccessorFlow,
            memorizedRepositoryIndexRepository,
            memorizedRepositoryMetadataRepository,
        )

    val accessorFlow =
        rawAccessor
            .mapAsLoadable()

    val localRepoAccessorFlow: Flow<OptionalLoadable<LocalRepo>> =
        optionalAccessorFlow
            .mapLoaded {
                if (it is LocalRepo) {
                    OptionalLoadable.LoadedAvailable(it)
                } else {
                    OptionalLoadable.NotAvailable(RuntimeException("Not a local repo: ${it.javaClass.simpleName}"))
                }
            }

    val informationFlow =
        combine(
            registeredRepositoryFlow,
            memorizingRepositoryReader.metadataFlow,
        ) { registryRepo, metadata ->
            RepositoryInformation(
                associationId = metadata.mapIfLoadedOrNull { it.associationGroupId },
                displayName = registryRepo?.displayLabel ?: uri.data,
            )
        }.shareResourceIn(scope)

    val resolvedState =
        combine(
            informationFlow,
            connectionStatusFlow,
        ) { info, connectionStatus ->
            ResolvedRepositoryState(uri, info, connectionStatus)
        }.shareResourceIn(scope)

    val indexFlow = memorizingRepositoryReader.indexFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    val localRepoStatus =
        localRepoAccessorFlow
            .flatMapLatestLoadedData { repo -> repo.observable.localIndex.mapToOptionalLoadable { it } }
            .stateIn(scope)

    suspend fun unlock(
        basicCredentials: BasicAuthCredentials,
        options: UnlockOptions,
    ) {
        val state: ProtectedLoadableResource<Repo, RepoAuthRequest> =
            rawAccessor
                .transformWhile {
                    if (it is ProtectedLoadableResource.Loading) {
                        true
                    } else {
                        emit(it)
                        false
                    }
                }.first()

        when (state) {
            is ProtectedLoadableResource.Loading ->
                throw RuntimeException("Shouldn't happen")

            is ProtectedLoadableResource.Failed -> {
                throw RuntimeException("Access failed")
            }

            is ProtectedLoadableResource.Loaded -> {
                throw RuntimeException("Doesn't need unlock")
            }

            is ProtectedLoadableResource.PendingAuthentication -> {
                state.authenticationRequest.tryOpen(basicCredentials, options)
            }
        }
    }

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

    suspend fun requireLoadedAccessor() =
        rawAccessor
            .transform {
                when (it) {
                    is ProtectedLoadableResource.Failed -> throw it.throwable
                    is ProtectedLoadableResource.Loaded -> emit(it.value)
                    ProtectedLoadableResource.Loading -> {}
                    is ProtectedLoadableResource.PendingAuthentication -> throw RepositoryLockedException(uri)
                }
            }.first()
}
