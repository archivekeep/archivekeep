package org.archivekeep.app.core.persistence.drivers.grpc

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.filterLoaded
import org.archivekeep.app.core.utils.firstLoadedOrNullOnErrorOrLocked
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.files.repo.remote.grpc.Options
import org.archivekeep.files.repo.remote.grpc.createPAT
import org.archivekeep.files.repo.remote.grpc.isNotAuthorized
import org.archivekeep.files.repo.remote.grpc.openGrpcArchive
import org.archivekeep.utils.loading.Loadable
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

    override fun openRepoFlow(uri: RepositoryURI) = flow { accessor(uri) }

    suspend fun FlowCollector<ProtectedLoadableResource<Repo, RepoAuthRequest>>.accessor(uri: RepositoryURI) {
        // TODO: map parallel, partial loadable - some filesystems might be slow or unresponsive

        // TODO: reuse opened

        val repoData = uri.typedRepoURIData as GRPCRepositoryURIData

        val insecure = true

        val options =
            Options(
                credentials = null,
                insecure = insecure,
            )

        val otherArchiveLocation = "grpc:${repoData.serialized()}"

        val credentialsFlow = credentialsStore.getRepositoryCredentialsFlow(uri)

        try {
            emit(ProtectedLoadableResource.Loaded(openGrpcArchive(otherArchiveLocation, options)))
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

                suspend fun tryAutoOpen(storedCredentials: BasicAuthCredentials?) {
                    if (storedCredentials != null) {
                        val result = openWithCredentials(storedCredentials)

                        successfulOpen.complete(result)
                    }
                }

                try {
                    tryAutoOpen(credentialsFlow.firstLoadedOrNullOnErrorOrLocked())
                } catch (e: Throwable) {
                    println("Auto-open failed: $e")
                }

                suspend fun tryOpen(
                    basicAuthCredentials: BasicAuthCredentials,
                    unlockOptions: UnlockOptions,
                ) {
                    var opened = openWithCredentials(basicAuthCredentials)

                    if (unlockOptions.rememberSession) {
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

                        credentialsStore.rememberRepositoryCredentials(uri, newCredentials)
                    }

                    successfulOpen.complete(opened)
                }

                if (!successfulOpen.isCompleted) {
                    emit(
                        ProtectedLoadableResource.PendingAuthentication(
                            RepoAuthRequest(tryOpen = ::tryOpen),
                        ),
                    )
                }

                coroutineScope {
                    credentialsFlow
                        .filterLoaded()
                        .onEach {
                            if (it.value != null) {
                                tryAutoOpen(it.value)
                            }
                        }.launchIn(CoroutineScope(SupervisorJob(coroutineContext.job)))

                    emit(ProtectedLoadableResource.Loaded(successfulOpen.await()))
                }
            } else {
                emit(ProtectedLoadableResource.Failed(e))
            }
        }
    }
}
