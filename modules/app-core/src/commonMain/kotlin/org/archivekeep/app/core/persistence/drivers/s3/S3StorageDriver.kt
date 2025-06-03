package org.archivekeep.app.core.persistence.drivers.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
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
import org.archivekeep.files.driver.s3.openS3Repository
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.stateIn
import java.net.URI

class S3StorageDriver(
    val scope: CoroutineScope,
    val credentialsStore: CredentialsStore,
) : StorageDriver {
    override fun getStorageAccessor(storageURI: StorageURI): StorageConnection =
        StorageConnection(
            storageURI,
            StorageInformation.OnlineStorage,
            flowOf(
                // TODO: implement ONLINE/OFFLINE status check
                Loadable.Loaded(Storage.ConnectionStatus.ONLINE),
            ).stateIn(scope),
        )

    override fun openRepoFlow(uri: RepositoryURI) =
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
                )

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
                val opened = openWithCredentials(basicAuthCredentials)

                if (unlockOptions.rememberSession) {
                    credentialsStore.rememberRepositoryCredentials(uri, basicAuthCredentials)
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
                    }.launchIn(CoroutineScope(SupervisorJob(coroutineContext.job)))

                emit(ProtectedLoadableResource.Loaded(successfulOpen.await()))
            }
        }.flowOn(Dispatchers.IO)
}
