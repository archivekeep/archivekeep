package org.archivekeep.app.core.persistence.drivers.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.repositories.asOptionalLoadable
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.RepositoryAccessorProvider
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.driver.s3.openS3Repository
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.ProtectedLoadableResource
import org.archivekeep.utils.loading.filterLoaded
import org.archivekeep.utils.loading.firstLoadedOrNullOnErrorOrLocked
import org.archivekeep.utils.loading.optional.stateIn
import org.archivekeep.utils.loading.stateIn
import java.net.URI

class S3StorageDriver(
    val scope: CoroutineScope,
    val credentialsStore: CredentialsStore,
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StorageDriver(S3RepositoryURIData.ID) {
    override fun getStorageAccessor(storageURI: StorageURI): StorageConnection =
        StorageConnection(
            storageURI,
            StorageInformation.OnlineStorage,
            flowOf(
                // TODO: implement ONLINE/OFFLINE status check
                Loadable.Loaded(Storage.ConnectionStatus.ONLINE),
            ).stateIn(scope),
        )

    val credentialsForUnlock = MutableStateFlow(emptyMap<RepositoryURI, BasicAuthCredentials>())

    override fun getProvider(uri: RepositoryURI): RepositoryAccessorProvider = RepositoryProvider(uri)

    inner class RepositoryProvider(
        uri: RepositoryURI,
    ) : RepositoryAccessorProvider {
        private val innerState = openRepoFlow(uri)

        override val repositoryAccessor: Flow<RepositoryAccessState> =
            innerState
                .map { it.asOptionalLoadable() }
                .stateIn(scope)
    }

    fun openRepoFlow(uri: RepositoryURI) =
        flow {
            // TODO: reuse opened
            val repoData = uri.typedRepoURIData as S3RepositoryURIData

            val credentialsFlow = credentialsStore.getRepositoryCredentialsFlow(uri)
            val successfulOpen = CompletableDeferred<Repo>()

            suspend fun openWithCredentials(basicAuthCredentials: BasicAuthCredentials): Repo =
                openS3Repository(
                    endpoint = URI.create(repoData.endpoint),
                    region = "TODO",
                    credentialsProvider =
                        StaticCredentialsProvider {
                            accessKeyId = basicAuthCredentials.username
                            secretAccessKey = basicAuthCredentials.password
                        },
                    repoData.bucket,
                ).also {
                    credentialsForUnlock.update { map ->
                        map
                            .toMutableMap()
                            .also { it[uri] = basicAuthCredentials }
                            .toMap()
                    }
                }

            suspend fun tryAutoOpen(storedCredentials: BasicAuthCredentials?) {
                if (storedCredentials != null) {
                    val result = openWithCredentials(storedCredentials)

                    successfulOpen.complete(result)
                }
            }

            try {
                tryAutoOpen(credentialsForUnlock.value[uri])
                tryAutoOpen(credentialsFlow.firstLoadedOrNullOnErrorOrLocked())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                println("Auto-open failed: $e")
                e.printStackTrace()
            }

            suspend fun tryOpen(
                basicAuthCredentials: BasicAuthCredentials,
                unlockOptions: UnlockOptions,
            ) {
                val opened = openWithCredentials(basicAuthCredentials)

                if (unlockOptions.rememberSession) {
                    credentialsStore.saveRepositoryCredentials(uri, basicAuthCredentials)
                }

                successfulOpen.complete(opened)
            }
            if (!successfulOpen.isCompleted) {
                this.emit(
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
                    }.launchIn(CoroutineScope(coroutineContext + SupervisorJob(coroutineContext.job)))

                emit(ProtectedLoadableResource.Loaded(successfulOpen.await()))
            }
        }.flowOn(ioDispatcher)
}
