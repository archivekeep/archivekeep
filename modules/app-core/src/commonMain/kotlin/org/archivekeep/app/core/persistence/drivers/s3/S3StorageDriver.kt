package org.archivekeep.app.core.persistence.drivers.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.RepositoryAccessorProvider
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.operations.RequiresCredentialsException
import org.archivekeep.app.core.persistence.credentials.ContentEncryptionPassword
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationDiscoveryForAdd
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver.PasswordRequest
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.driver.s3.S3Repository
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.filterLoaded
import org.archivekeep.utils.loading.firstLoadedOrNullOnErrorOrLocked
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable.LoadedAvailable
import org.archivekeep.utils.loading.optional.OptionalLoadable.NotAvailable
import org.archivekeep.utils.loading.optional.flatMapLatestLoadedData
import org.archivekeep.utils.loading.optional.stateIn
import org.archivekeep.utils.loading.stateIn
import java.net.URI

class S3StorageDriver(
    private val scope: CoroutineScope,
    private val credentialsStore: CredentialsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
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

    private val credentialsForUnlock = MutableStateFlow(emptyMap<RepositoryURI, BasicAuthCredentials>())

    override fun getProvider(uri: RepositoryURI): RepositoryAccessorProvider = RepositoryProvider(uri)

    private sealed interface InnerState {
        val repoStateFlow: Flow<RepositoryAccessState>

        class PlainS3Repository(
            val repo: S3Repository,
        ) : InnerState {
            override val repoStateFlow = flowOf(LoadedAvailable(this.repo))
        }

        class EncryptedS3Repository(
            val repositoryURI: RepositoryURI,
            val repo: org.archivekeep.files.driver.s3.EncryptedS3Repository,
            val credentialsStore: CredentialsStore,
            val scope: CoroutineScope,
        ) : InnerState {
            val opened = MutableStateFlow<org.archivekeep.files.driver.s3.EncryptedS3Repository?>(null)

            val passwordRequest =
                PasswordRequest { providedPassword, rememberPassword ->
                    repo.unlock(providedPassword)

                    opened.value = repo

                    if (rememberPassword) {
                        credentialsStore.saveRepositorySecret(repositoryURI, ContentEncryptionPassword, JsonPrimitive(providedPassword))
                    }
                }

            val autoOpenFlow =
                flow<Unit> {
                    credentialsStore
                        .getRepositorySecretsFlow(repositoryURI)
                        .map { optionalCredentials ->
                            println("Received credentials: $optionalCredentials")

                            when (optionalCredentials) {
                                OptionalLoadable.Loading -> false
                                is OptionalLoadable.Failed -> false
                                is LoadedAvailable -> {
                                    optionalCredentials.value[ContentEncryptionPassword]?.let { contentEncryptionPassword ->
                                        if (opened.value == null) {
                                            try {
                                                passwordRequest.providePassword(contentEncryptionPassword.jsonPrimitive.content, false)

                                                return@map true
                                            } catch (e: Throwable) {
                                                // TODO: expose failure to UI, or move auto-unlocking responsibility out of driver logic
                                                println("Auto-unlock failed: $e")
                                            }
                                        }
                                    }

                                    false
                                }
                                is NotAvailable -> false
                            }
                        }.takeWhile { success -> !success }
                        .collect {}
                }.shareIn(scope, SharingStarted.WhileSubscribed(0, 0))

            override val repoStateFlow: Flow<RepositoryAccessState> =
                channelFlow {
                    val autoOpenCollector = launch(start = CoroutineStart.LAZY) { autoOpenFlow.collect {} }

                    opened
                        .onStart { autoOpenCollector.start() }
                        .onCompletion { autoOpenCollector.cancel() }
                        .collect { repo ->
                            send(
                                if (repo != null) {
                                    LoadedAvailable(repo)
                                } else {
                                    NeedsUnlock(passwordRequest)
                                },
                            )
                        }
                }
        }

        class NotRepository(
            val cause: Exception,
        ) : InnerState {
            override val repoStateFlow = flowOf(OptionalLoadable.Failed(this.cause))
        }
    }

    private inner class RepositoryProvider(
        uri: RepositoryURI,
    ) : RepositoryAccessorProvider {
        private val internalStateFlow = openRepoFlow(uri).stateIn(scope)

        @OptIn(ExperimentalCoroutinesApi::class)
        override val repositoryAccessor = internalStateFlow.flatMapLatestLoadedData { it.repoStateFlow }
    }

    override suspend fun discoverRepository(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials?,
    ): RepositoryLocationDiscoveryForAdd {
        if (credentials == null) {
            throw RequiresCredentialsException()
        }

        val repoData = uri.typedRepoURIData as S3RepositoryURIData

        val result =
            S3RepositoryLocation
                .open(
                    endpoint = URI.create(repoData.endpoint),
                    region = "TODO",
                    credentialsProvider =
                        StaticCredentialsProvider {
                            accessKeyId = credentials.username
                            secretAccessKey = credentials.password
                        },
                    bucketName = repoData.bucket,
                    onPreserveCredentials = { rememberCredentials ->
                        if (rememberCredentials) {
                            credentialsStore.saveRepositoryCredentials(uri, credentials)
                        } else {
                            credentialsForUnlock.update { map ->
                                map
                                    .toMutableMap()
                                    .also { it[uri] = credentials }
                                    .toMap()
                            }
                        }
                    },
                )

        return result
    }

    private fun openRepoFlow(uri: RepositoryURI): Flow<OptionalLoadable<InnerState>> =
        flow<OptionalLoadable<InnerState>> {
            // TODO: reuse opened
            val repoData = uri.typedRepoURIData as S3RepositoryURIData

            val credentialsFlow = credentialsStore.getRepositoryCredentialsFlow(uri)
            val successfulOpen = CompletableDeferred<S3RepositoryLocation>()

            suspend fun openWithCredentials(basicAuthCredentials: BasicAuthCredentials): S3RepositoryLocation =
                S3RepositoryLocation
                    .open(
                        endpoint = URI.create(repoData.endpoint),
                        region = "TODO",
                        credentialsProvider =
                            StaticCredentialsProvider {
                                accessKeyId = basicAuthCredentials.username
                                secretAccessKey = basicAuthCredentials.password
                            },
                        bucketName = repoData.bucket,
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
                    NeedsUnlock(RepoAuthRequest(tryOpen = ::tryOpen)),
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

                val innerState =
                    when (val result = successfulOpen.await()) {
                        is S3RepositoryLocation.LocationCanBeInitialized -> InnerState.NotRepository(RuntimeException("Just not repository"))
                        is S3RepositoryLocation.ContainingEncryptedS3Repository ->
                            InnerState.EncryptedS3Repository(
                                uri,
                                result.connection,
                                credentialsStore,
                                scope,
                            )
                        is S3RepositoryLocation.ContainingS3Repository -> InnerState.PlainS3Repository(result.connection)
                    }

                emit(LoadedAvailable(innerState))
            }
        }.flowOn(ioDispatcher)
}
