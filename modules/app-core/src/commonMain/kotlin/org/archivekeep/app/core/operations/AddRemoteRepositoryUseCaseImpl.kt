package org.archivekeep.app.core.operations

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformWhile
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.getDriverForURI
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationContentsState
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials
import org.archivekeep.utils.loading.optional.OptionalLoadable

class AddRemoteRepositoryUseCaseImpl(
    val repositoryService: RepositoryService,
    val registry: RegistryDataStore,
    val drivers: Map<String, StorageDriver>,
) : AddRemoteRepositoryUseCase {
    override suspend fun invoke(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials?,
        rememberCredentials: Boolean,
    ): AddRemoteRepositoryOutcome {
        val driver = drivers.getDriverForURI(uri) ?: throw RuntimeException("Driver unsupported")
        val location = driver.openLocation(uri)

        val locationContents: RepositoryLocationContentsState =
            location
                .locationContents
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
                                when (it.unlockRequest) {
                                    is RepoAuthRequest -> {
                                        it.unlockRequest.tryOpen(credentials!!, UnlockOptions(false))
                                        true
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
                locationContents.preserveCredentials(rememberCredentials)

                repositoryService.registerRepository(repositoryURI = uri)

                return AddRemoteRepositoryOutcome.Added
            }
            is RepositoryLocationContentsState.LocationCanBeInitialized -> {
                return AddRemoteRepositoryOutcome.NeedsInitialization(
                    initializeAsPlain =
                        locationContents.initializeAsPlain?.let { initializeAsPlain ->
                            {
                                initializeAsPlain(rememberCredentials)

                                repositoryService.registerRepository(repositoryURI = uri)
                            }
                        },
                    initializeAsE2EEPasswordProtected =
                        locationContents.initializeAsE2EEPasswordProtected?.let { initializeAsE2EEPasswordProtected ->
                            { password ->
                                initializeAsE2EEPasswordProtected(password, rememberCredentials)

                                repositoryService.registerRepository(repositoryURI = uri)
                            }
                        },
                )
            }
        }
    }
}
