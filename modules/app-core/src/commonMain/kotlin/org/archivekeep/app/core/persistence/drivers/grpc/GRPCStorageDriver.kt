package org.archivekeep.app.core.persistence.drivers.grpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.archivekeep.app.core.api.repository.location.RepositoryLocationAccessor
import org.archivekeep.app.core.api.repository.location.RepositoryLocationContentsState
import org.archivekeep.app.core.api.repository.location.UserCredentialsRequest
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.files.repo.remote.grpc.Options
import org.archivekeep.files.repo.remote.grpc.createPAT
import org.archivekeep.files.repo.remote.grpc.isNotAuthorized
import org.archivekeep.files.repo.remote.grpc.openGrpcArchive
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.stateIn
import org.archivekeep.utils.loading.stateIn

class GRPCStorageDriver(
    val scope: CoroutineScope,
    val credentialsStore: CredentialsStore,
) : StorageDriver(GRPCRepositoryURIData.ID) {
    override fun getStorageAccessor(storageURI: StorageURI): StorageConnection =
        StorageConnection(
            storageURI,
            StorageInformation.OnlineStorage,
            flowOf(
                // TODO: implement ONLINE/OFFLINE status check
                Loadable.Loaded(Storage.ConnectionStatus.ONLINE),
            ).stateIn(scope),
        )

    override fun openLocation(uri: RepositoryURI): RepositoryLocationAccessor = RepositoryProvider(uri)

    inner class RepositoryProvider(
        uri: RepositoryURI,
    ) : RepositoryLocationAccessor {
        override val contentsStateFlow: Flow<OptionalLoadable<RepositoryLocationContentsState>> =
            openRepoFlow(uri).stateIn(scope)
    }

    fun openRepoFlow(uri: RepositoryURI): Flow<OptionalLoadable<RepositoryLocationContentsState>> =
        flow {
            val state = MutableStateFlow<OptionalLoadable<RepositoryLocationContentsState>>(OptionalLoadable.Loading)

            val repoData = uri.typedRepoURIData as GRPCRepositoryURIData

            val insecure = true

            val options =
                Options(
                    credentials = null,
                    insecure = insecure,
                )

            val otherArchiveLocation = "grpc:${repoData.serialized()}"

            try {
                val repo = openGrpcArchive(otherArchiveLocation, options)

                state.value =
                    OptionalLoadable.LoadedAvailable(
                        object : RepositoryLocationContentsState.IsRepositoryLocation {
                            override val repoStateFlow: Flow<RepositoryAccessState> = flowOf(OptionalLoadable.LoadedAvailable(repo))
                        },
                    )
            } catch (e: Exception) {
                if (e.isNotAuthorized()) {
                    suspend fun openWithCredentials(basicAuthCredentials: BasicAuthCredentials): Repo {
                        val optionsWithCredentials =
                            options.copy(
                                credentials = basicAuthCredentials,
                            )

                        return openGrpcArchive(otherArchiveLocation, optionsWithCredentials)
                    }

                    suspend fun tryOpen(
                        basicAuthCredentials: BasicAuthCredentials,
                        unlockOptions: UnlockOptions,
                    ) {
                        var opened = openWithCredentials(basicAuthCredentials)

                        if (unlockOptions.permanentCredentialsPreserve) {
                            val newCredentials =
                                createPAT(
                                    repoData.hostname,
                                    repoData.port.toInt(),
                                    // TODO
                                    true,
                                    basicAuthCredentials,
                                )

                            opened =
                                openGrpcArchive(
                                    otherArchiveLocation,
                                    options.copy(
                                        credentials = newCredentials,
                                    ),
                                )

                            credentialsStore.saveRepositoryCredentials(uri, newCredentials)
                        }

                        state.value =
                            OptionalLoadable.LoadedAvailable(
                                object : RepositoryLocationContentsState.IsRepositoryLocation {
                                    override val repoStateFlow: Flow<RepositoryAccessState> = flowOf(OptionalLoadable.LoadedAvailable(opened))
                                },
                            )
                    }

                    state.value = NeedsUnlock(UserCredentialsRequest(tryOpen = ::tryOpen))
                } else {
                    state.value = OptionalLoadable.Failed(e)
                }
            }

            emitAll(state)
        }
}
