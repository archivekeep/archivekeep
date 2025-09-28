package org.archivekeep.app.core.persistence.drivers.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.persistence.credentials.ContentEncryptionPassword
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationAccessor
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationContentsState
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationContentsState.IsRepositoryLocation
import org.archivekeep.app.core.persistence.drivers.autoUnlocker
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver.PasswordRequest
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver.InnerState.LocationCanBeInitialized
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver.InnerState.PlainS3Repository
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.driver.s3.EncryptedS3Repository
import org.archivekeep.files.driver.s3.S3LocationNotInitializedAsRepositoryException
import org.archivekeep.files.driver.s3.S3Repository
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable.LoadedAvailable
import org.archivekeep.utils.loading.optional.OptionalLoadable.NotAvailable
import org.archivekeep.utils.loading.optional.flatMapLatestLoadedData
import org.archivekeep.utils.loading.optional.mapLoadedData
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

    override fun openLocation(uri: RepositoryURI): RepositoryLocationAccessorImpl = RepositoryLocationAccessorImpl(uri)

    internal sealed interface InnerState {
        val repositoryLocationContentsState: RepositoryLocationContentsState
        val repoStateFlow: Flow<RepositoryAccessState>

        class PlainS3Repository(
            val repo: S3Repository,
            override val repositoryLocationContentsState: IsRepositoryLocation,
        ) : InnerState {
            override val repoStateFlow = flowOf(LoadedAvailable(this.repo))
        }

        class EncryptedS3Repository(
            val repositoryURI: RepositoryURI,
            val repo: org.archivekeep.files.driver.s3.EncryptedS3Repository,
            val credentialsStore: CredentialsStore,
            val scope: CoroutineScope,
            override val repositoryLocationContentsState: IsRepositoryLocation,
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

        class LocationCanBeInitialized(
            val initializeAsPlain: (suspend (permanentPreserve: Boolean) -> Unit)?,
            val initializeAsE2EEPasswordProtected: (suspend (password: String, permanentPreserve: Boolean) -> Unit)?,
        ) : InnerState {
            override val repositoryLocationContentsState =
                RepositoryLocationContentsState.LocationCanBeInitialized(
                    initializeAsPlain,
                    initializeAsE2EEPasswordProtected,
                )

            override val repoStateFlow = flowOf(OptionalLoadable.Failed(RuntimeException("Not repository")))
        }
    }

    private suspend fun open(
        repositoryURI: RepositoryURI,
        endpoint: URI,
        region: String,
        credentialsProvider: CredentialsProvider,
        bucketName: String,
        onPreserveCredentials: suspend (permanentPreserve: Boolean) -> Unit,
    ): InnerState {
        try {
            val connection =
                EncryptedS3Repository
                    .open(endpoint, region, credentialsProvider, bucketName)

            return InnerState.EncryptedS3Repository(
                repositoryURI,
                connection,
                credentialsStore,
                scope,
                object : IsRepositoryLocation {
                    override suspend fun preserveCredentials(permanentPreserve: Boolean) {
                        onPreserveCredentials(permanentPreserve)
                    }
                },
            )
        } catch (_: S3LocationNotInitializedAsRepositoryException) {
            // ignore, it's one of happy paths for discovery
        }

        try {
            val connection =
                S3Repository.open(endpoint, region, credentialsProvider, bucketName)

            return PlainS3Repository(
                connection,
                object : IsRepositoryLocation {
                    override suspend fun preserveCredentials(permanentPreserve: Boolean) {
                        onPreserveCredentials(permanentPreserve)
                    }
                },
            )
        } catch (_: S3LocationNotInitializedAsRepositoryException) {
            // ignore, it's one of happy paths for discovery
        }

        return LocationCanBeInitialized(
            initializeAsPlain = { permanentPreserve ->
                S3Repository.create(endpoint, region, credentialsProvider, bucketName)
                onPreserveCredentials(permanentPreserve)
            },
            initializeAsE2EEPasswordProtected = { password, permanentPreserve ->
                EncryptedS3Repository.create(endpoint, region, credentialsProvider, bucketName, password)
                onPreserveCredentials(permanentPreserve)
            },
        )
    }

    inner class RepositoryLocationAccessorImpl(
        uri: RepositoryURI,
    ) : RepositoryLocationAccessor {
        internal val internalStateFlow =
            flow {
                val state = MutableStateFlow<OptionalLoadable<InnerState>>(OptionalLoadable.Loading)
                val openLock = Mutex()

                val repoData = uri.typedRepoURIData as S3RepositoryURIData

                suspend fun tryOpen(
                    basicAuthCredentials: BasicAuthCredentials,
                    unlockOptions: UnlockOptions,
                ) {
                    openLock.withLock {
                        if (state.value is LoadedAvailable) {
                            // already unlocked
                            return
                        }

                        val credentialsPreserve: suspend (permanentPreserve: Boolean) -> Unit = { permanentPreserve ->
                            if (permanentPreserve) {
                                credentialsStore.saveRepositoryCredentials(uri, basicAuthCredentials)
                            } else {
                                credentialsForUnlock.update { map ->
                                    map
                                        .toMutableMap()
                                        .also { it[uri] = basicAuthCredentials }
                                        .toMap()
                                }
                            }
                        }

                        val opened =
                            open(
                                uri,
                                endpoint = URI.create(repoData.endpoint),
                                region = "TODO",
                                credentialsProvider =
                                    StaticCredentialsProvider {
                                        accessKeyId = basicAuthCredentials.username
                                        secretAccessKey = basicAuthCredentials.password
                                    },
                                bucketName = repoData.bucket,
                                onPreserveCredentials = credentialsPreserve,
                            )

                        if (unlockOptions.preserveCredentials) {
                            credentialsPreserve(unlockOptions.permanentCredentialsPreserve)
                        }

                        state.value = LoadedAvailable(opened)
                    }
                }

                state.value = NeedsUnlock(RepoAuthRequest(tryOpen = ::tryOpen))

                emitAll(state)
            }.flowOn(ioDispatcher).stateIn(scope)

        override val locationContents: Flow<OptionalLoadable<RepositoryLocationContentsState>> =
            internalStateFlow.mapLoadedData {
                it.repositoryLocationContentsState
            }

        @OptIn(ExperimentalCoroutinesApi::class)
        override val repositoryAccessor = internalStateFlow.flatMapLatestLoadedData { it.repoStateFlow }

        override val autoUnlockRepositoryAccessor: Flow<RepositoryAccessState> = repositoryAccessor.autoUnlocker(uri, credentialsForUnlock, credentialsStore)
    }
}
