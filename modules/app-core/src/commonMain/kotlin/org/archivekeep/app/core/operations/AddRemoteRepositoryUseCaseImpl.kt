package org.archivekeep.app.core.operations

import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.getDriverForURI
import org.archivekeep.app.core.persistence.drivers.RepositoryLocationDiscoveryOutcome
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

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
        val discoveryResult = driver.discoverRepository(uri, credentials)

        when (val outcome = discoveryResult.asRepositoryLocationDiscoveryOutcome()) {
            RepositoryLocationDiscoveryOutcome.IsRepositoryLocation -> {
                discoveryResult.preserveCredentialss(rememberCredentials)

                repositoryService.registerRepository(repositoryURI = uri)

                return AddRemoteRepositoryOutcome.Added
            }
            is RepositoryLocationDiscoveryOutcome.LocationCanBeInitialized -> {
                return AddRemoteRepositoryOutcome.NeedsInitialization(
                    initializeAsPlain =
                        outcome.initializeAsPlain?.let { initializeAsPlain ->
                            {
                                initializeAsPlain()

                                discoveryResult.preserveCredentialss(rememberCredentials)

                                repositoryService.registerRepository(repositoryURI = uri)
                            }
                        },
                    initializeAsE2EEPasswordProtected =
                        outcome.initializeAsE2EEPasswordProtected?.let { initializeAsE2EEPasswordProtected ->
                            { password ->
                                initializeAsE2EEPasswordProtected(password)

                                discoveryResult.preserveCredentialss(rememberCredentials)

                                repositoryService.registerRepository(repositoryURI = uri)
                            }
                        },
                )
            }
        }
    }
}
