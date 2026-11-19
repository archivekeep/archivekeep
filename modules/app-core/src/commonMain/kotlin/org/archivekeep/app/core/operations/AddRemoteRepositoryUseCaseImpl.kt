package org.archivekeep.app.core.operations

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import org.archivekeep.app.core.api.repository.location.GoogleDriveCredentialsJsonRequest
import org.archivekeep.app.core.api.repository.location.RepositoryLocationContentsState
import org.archivekeep.app.core.api.repository.location.UserCredentialsRequest
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.getDriverForURI
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI
import org.archivekeep.files.api.repository.auth.BasicAuthCredentials
import org.archivekeep.utils.loading.optional.OptionalLoadable

class AddRemoteRepositoryUseCaseImpl(
    val repositoryService: RepositoryService,
    val registry: RegistryDataStore,
    val credentialsStore: CredentialsStore,
    val drivers: Map<String, StorageDriver>,
) : AddRemoteRepositoryUseCase {
    override suspend fun invoke(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials?,
        rememberCredentials: Boolean,
        label: String?,
    ): AddRemoteRepositoryOutcome = addRemote(uri, credentials, rememberCredentials, null, label)

    override suspend fun addGoogleDrive(
        uri: RepositoryURI,
        credentialsJson: String,
        label: String?,
    ): AddRemoteRepositoryOutcome {
        val result = addRemote(uri, null, false, credentialsJson, label)
        val storageURI = uri.typedRepoURIData.storageURI
        when (result) {
            is AddRemoteRepositoryOutcome.Added -> {
                ensureStorageRegistered(storageURI)
            }
            is AddRemoteRepositoryOutcome.NeedsInitialization -> {
                AddRemoteRepositoryOutcome.NeedsInitialization(
                    initializeAsPlain =
                        result.initializeAsPlain?.let { initializeAsPlain ->
                            {
                                initializeAsPlain()
                                ensureStorageRegistered(storageURI)
                            }
                        },
                    initializeAsE2EEPasswordProtected =
                        result.initializeAsE2EEPasswordProtected?.let { initializeAsE2EEPasswordProtected ->
                            { password ->
                                initializeAsE2EEPasswordProtected(password)
                                ensureStorageRegistered(storageURI)
                            }
                        },
                )
            }
        }
        return result
    }

    private suspend fun ensureStorageRegistered(storageURI: StorageURI) {
        registry.updateStorage(storageURI) {
            it.copy(
                label = "Google Drive",
                isLocal = false,
            )
        }
    }

    private suspend fun addRemote(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials?,
        rememberCredentials: Boolean,
        googleDriveCredentialsJson: String?,
        label: String?,
    ): AddRemoteRepositoryOutcome {
        val driver = drivers.getDriverForURI(uri) ?: throw RuntimeException("Driver unsupported")
        val location = driver.openLocation(uri)

        val locationContents: RepositoryLocationContentsState =
            location
                .contentsStateFlow
                .transformWhile {
                    when (it) {
                        OptionalLoadable.Loading -> true
                        is OptionalLoadable.Failed -> throw RuntimeException(it.cause)
                        is OptionalLoadable.LoadedAvailable -> {
                            emit(it.value)
                            false
                        }

                        is OptionalLoadable.NotAvailable -> {
                            if (it is NeedsUnlock) {
                                when (val unlockRequest = it.unlockRequest) {
                                    is UserCredentialsRequest -> {
                                        if (credentials != null) {
                                            // TODO: move out this
                                            unlockRequest.tryOpen(
                                                credentials,
                                                UnlockOptions(rememberCredentials, rememberCredentials),
                                            )
                                            true
                                        } else {
                                            throw RequiresCredentialsException()
                                        }
                                    }

                                    is GoogleDriveCredentialsJsonRequest -> {
                                        if (googleDriveCredentialsJson != null) {
                                            unlockRequest.tryOpen(googleDriveCredentialsJson, UnlockOptions(false, false))
                                            true
                                        } else {
                                            throw RequiresCredentialsException()
                                        }
                                    }

                                    else -> throw RuntimeException("Unsupported: ${it.unlockRequest.javaClass}")
                                }
                            } else {
                                throw RuntimeException("Unsupported: ${it.javaClass}")
                            }
                        }
                    }
                }.first()

        when (locationContents) {
            is RepositoryLocationContentsState.IsRepositoryLocation -> {
                // TODO: move here permanent preserve
                if (!rememberCredentials && credentials != null) {
                    credentialsStore
                        .inMemoryCredentials
                        .update { map ->
                            map
                                .toMutableMap()
                                .also { it[uri] = credentials }
                                .toMap()
                        }
                }

                repositoryService.registerRepository(repositoryURI = uri, label = label)

                return AddRemoteRepositoryOutcome.Added
            }

            is RepositoryLocationContentsState.LocationCanBeInitialized -> {
                return AddRemoteRepositoryOutcome.NeedsInitialization(
                    initializeAsPlain =
                        locationContents.initializeAsPlain?.let { initializeAsPlain ->
                            {
                                initializeAsPlain(rememberCredentials)

                                repositoryService.registerRepository(repositoryURI = uri, label = label)
                            }
                        },
                    initializeAsE2EEPasswordProtected =
                        locationContents.initializeAsE2EEPasswordProtected?.let { initializeAsE2EEPasswordProtected ->
                            { password ->
                                initializeAsE2EEPasswordProtected(password, rememberCredentials)

                                repositoryService.registerRepository(repositoryURI = uri, label = label)
                            }
                        },
                )
            }
        }
    }
}
