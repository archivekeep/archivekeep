package org.archivekeep.app.core.persistence.drivers.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive
import org.archivekeep.app.core.api.repository.location.PasswordRequest
import org.archivekeep.app.core.api.repository.location.RepositoryLocationAccessor
import org.archivekeep.app.core.api.repository.location.RepositoryLocationContentsState
import org.archivekeep.app.core.api.repository.location.RepositoryLocationContentsState.IsRepositoryLocation
import org.archivekeep.app.core.api.repository.location.UserCredentialsRequest
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.RepositoryAccessState
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.persistence.credentials.ContentEncryptionPassword
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver.InnerState.LocationCanBeInitialized
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver.InnerState.PlainS3Repository
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.driver.s3.EncryptedS3Repository
import org.archivekeep.files.driver.s3.S3LocationNotInitializedAsRepositoryException
import org.archivekeep.files.driver.s3.S3Repository
import org.archivekeep.files.repo.auth.BasicAuthCredentials
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable.LoadedAvailable
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

    override fun openLocation(uri: RepositoryURI): RepositoryLocationAccessorImpl = RepositoryLocationAccessorImpl(uri)

    internal sealed interface InnerState {
        val repositoryLocationContentsState: RepositoryLocationContentsState

        class PlainS3Repository(val repo: S3Repository) : InnerState {
            override val repositoryLocationContentsState: IsRepositoryLocation =
                object : IsRepositoryLocation {
                    override val repoStateFlow = flowOf(LoadedAvailable(this@PlainS3Repository.repo))
                }
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

            override val repositoryLocationContentsState =
                object : IsRepositoryLocation {
                    override val repoStateFlow: Flow<RepositoryAccessState> =
                        opened.map { repo ->
                            if (repo != null) {
                                LoadedAvailable(repo)
                            } else {
                                NeedsUnlock(passwordRequest)
                            }
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
        }
    }

    private suspend fun open(
        repositoryURI: RepositoryURI,
        endpoint: URI,
        region: String,
        credentialsProvider: CredentialsProvider,
        bucketName: String,
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
            )
        } catch (_: S3LocationNotInitializedAsRepositoryException) {
            // ignore, it's one of happy paths for discovery
        }

        try {
            val connection = S3Repository.open(endpoint, region, credentialsProvider, bucketName)

            return PlainS3Repository(connection)
        } catch (_: S3LocationNotInitializedAsRepositoryException) {
            // ignore, it's one of happy paths for discovery
        }

        return LocationCanBeInitialized(
            initializeAsPlain = { permanentPreserve ->
                S3Repository.create(endpoint, region, credentialsProvider, bucketName)
            },
            initializeAsE2EEPasswordProtected = { password, permanentPreserve ->
                EncryptedS3Repository.create(endpoint, region, credentialsProvider, bucketName, password)
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
                                credentialsStore.inMemoryCredentials.update { map ->
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
                            )

                        if (unlockOptions.preserveCredentials) {
                            credentialsPreserve(unlockOptions.permanentCredentialsPreserve)
                        }

                        state.value = LoadedAvailable(opened)
                    }
                }

                state.value = NeedsUnlock(UserCredentialsRequest(tryOpen = ::tryOpen))

                emitAll(state)
            }.flowOn(ioDispatcher).stateIn(scope)

        override val contentsStateFlow: Flow<OptionalLoadable<RepositoryLocationContentsState>> =
            internalStateFlow.mapLoadedData {
                it.repositoryLocationContentsState
            }
    }
}
