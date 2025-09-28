package org.archivekeep.app.core.persistence.drivers.grpc

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.repositories.asOptionalLoadable
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationAccessor
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationContentsState
import org.archivekeep.app.core.persistence.drivers.autoUnlocker
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.files.repo.remote.grpc.Options
import org.archivekeep.files.repo.remote.grpc.createPAT
import org.archivekeep.files.repo.remote.grpc.isNotAuthorized
import org.archivekeep.files.repo.remote.grpc.openGrpcArchive
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.ProtectedLoadableResource
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
        private val innerState = openRepoFlow(uri)

        override val locationContents: Flow<OptionalLoadable<RepositoryLocationContentsState>>
            get() = TODO("Not yet implemented")

        override val repositoryAccessor: Flow<RepositoryAccessState> =
            innerState.map { it.asOptionalLoadable() }.stateIn(scope)

        override val autoUnlockRepositoryAccessor: Flow<RepositoryAccessState> =
            repositoryAccessor.autoUnlocker(
                uri,
                // TODO: add support
                MutableStateFlow(mapOf()),
                credentialsStore,
            )
    }

    fun openRepoFlow(uri: RepositoryURI) = flow { accessor(uri) }

    suspend fun FlowCollector<ProtectedLoadableResource<Repo, RepoAuthRequest>>.accessor(uri: RepositoryURI) {
        val state = MutableStateFlow<ProtectedLoadableResource<Repo, RepoAuthRequest>>(ProtectedLoadableResource.Loading)
        val openLock = Mutex()

        val repoData = uri.typedRepoURIData as GRPCRepositoryURIData

        val insecure = true

        val options =
            Options(
                credentials = null,
                insecure = insecure,
            )

        val otherArchiveLocation = "grpc:${repoData.serialized()}"

        try {
            state.value = ProtectedLoadableResource.Loaded(openGrpcArchive(otherArchiveLocation, options))
        } catch (e: Exception) {
            if (e.isNotAuthorized()) {
                val successfulOpen = CompletableDeferred<Repo>()

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

                    successfulOpen.complete(opened)
                }

                state.value = ProtectedLoadableResource.PendingAuthentication(RepoAuthRequest(tryOpen = ::tryOpen))
            } else {
                state.value = ProtectedLoadableResource.Failed(e)
            }
        }

        emitAll(state)
    }
}
