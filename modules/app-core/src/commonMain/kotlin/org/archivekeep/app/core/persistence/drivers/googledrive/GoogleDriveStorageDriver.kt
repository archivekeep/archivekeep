package org.archivekeep.app.core.persistence.drivers.googledrive

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.archivekeep.app.core.api.repository.location.RepositoryLocationAccessor
import org.archivekeep.app.core.api.repository.location.RepositoryLocationContentsState
import org.archivekeep.app.core.api.repository.location.UserCredentialsInputType
import org.archivekeep.app.core.api.repository.location.UserCredentialsRequest
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.Storage
import org.archivekeep.app.core.domain.storages.StorageConnection
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageInformation
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.api.repository.Repo
import org.archivekeep.files.api.repository.auth.BasicAuthCredentials
import org.archivekeep.files.driver.googledrive.GoogleDriveLocationNotInitializedAsRepositoryException
import org.archivekeep.files.driver.googledrive.GoogleDriveRepository
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.optional.OptionalLoadable
import org.archivekeep.utils.loading.optional.OptionalLoadable.LoadedAvailable
import org.archivekeep.utils.loading.optional.mapLoadedData
import org.archivekeep.utils.loading.optional.stateIn
import org.archivekeep.utils.loading.stateIn

class GoogleDriveStorageDriver internal constructor(
    private val scope: CoroutineScope,
    private val credentialsStore: CredentialsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val authProvider: GoogleDriveAuthProvider,
) : StorageDriver(GoogleDriveRepositoryURIData.ID) {
    override fun getStorageAccessor(storageURI: StorageURI): StorageConnection =
        StorageConnection(
            storageURI,
            StorageInformation.OnlineStorage,
            flowOf(
                // TODO: implement ONLINE/OFFLINE status check
                Loadable.Loaded(Storage.ConnectionStatus.ONLINE),
            ).stateIn(scope),
        )

    override fun openLocation(uri: RepositoryURI): RepositoryLocationAccessor = RepositoryLocationAccessorImpl(uri)

    internal sealed interface InnerState {
        val repositoryLocationContentsState: RepositoryLocationContentsState

        class PlainGoogleDriveRepository(
            repo: Repo,
        ) : InnerState {
            override val repositoryLocationContentsState: RepositoryLocationContentsState.IsRepositoryLocation =
                object : RepositoryLocationContentsState.IsRepositoryLocation {
                    override val repoStateFlow = flowOf(LoadedAvailable(repo))
                }
        }

        class LocationCanBeInitialized(
            initializeAsPlain: suspend (permanentPreserve: Boolean) -> Unit,
        ) : InnerState {
            override val repositoryLocationContentsState =
                RepositoryLocationContentsState.LocationCanBeInitialized(
                    initializeAsPlain = initializeAsPlain,
                    initializeAsE2EEPasswordProtected = null,
                )
        }
    }

    private suspend fun open(
        rootFolderId: String,
        credentialsJson: String,
    ): InnerState =
        try {
            InnerState.PlainGoogleDriveRepository(
                withContext(Dispatchers.IO) {
                    GoogleDriveRepository.open(authProvider.createDriveClient(credentialsJson), rootFolderId)
                },
            )
        } catch (_: GoogleDriveLocationNotInitializedAsRepositoryException) {
            InnerState.LocationCanBeInitialized { permanentPreserve ->
                withContext(Dispatchers.IO) {
                    GoogleDriveRepository.create(authProvider.createDriveClient(credentialsJson), rootFolderId).close()
                }
                preserveCredentials(
                    RepositoryURI(GoogleDriveRepositoryURIData.ID, rootFolderId),
                    // TODO: not basic auth credentials
                    BasicAuthCredentials(username = "", password = credentialsJson),
                    permanentPreserve,
                )
            }
        }

    private suspend fun preserveCredentials(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials,
        permanentPreserve: Boolean,
    ) {
        if (permanentPreserve) {
            credentialsStore.saveRepositoryCredentials(uri, credentials)
        } else {
            credentialsStore.inMemoryCredentials.update { existing -> existing + (uri to credentials) }
        }
    }

    inner class RepositoryLocationAccessorImpl(
        uri: RepositoryURI,
    ) : RepositoryLocationAccessor {
        internal val internalStateFlow =
            flow {
                val state = MutableStateFlow<OptionalLoadable<InnerState>>(OptionalLoadable.Loading)
                val openLock = Mutex()
                val repoData = uri.typedRepoURIData as GoogleDriveRepositoryURIData

                suspend fun tryOpen(
                    // TODO: not basic auth credentials
                    credentials: BasicAuthCredentials,
                    unlockOptions: UnlockOptions,
                ) {
                    openLock.withLock {
                        if (state.value is LoadedAvailable) {
                            return
                        }

                        val opened = open(repoData.rootFolderId, credentials.password)

                        if (unlockOptions.preserveCredentials) {
                            preserveCredentials(uri, credentials, unlockOptions.permanentCredentialsPreserve)
                        }

                        state.value = LoadedAvailable(opened)
                    }
                }

                state.value =
                    NeedsUnlock(
                        UserCredentialsRequest(
                            tryOpen = ::tryOpen,
                            inputType = UserCredentialsInputType.GOOGLE_DRIVE_CREDENTIALS_JSON,
                        ),
                    )

                emitAll(state)
            }.flowOn(ioDispatcher).stateIn(scope)

        override val contentsStateFlow: Flow<OptionalLoadable<RepositoryLocationContentsState>> =
            internalStateFlow.mapLoadedData { it.repositoryLocationContentsState }
    }
}
