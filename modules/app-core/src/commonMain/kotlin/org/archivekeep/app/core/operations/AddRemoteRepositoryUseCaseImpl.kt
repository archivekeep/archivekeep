package org.archivekeep.app.core.operations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

class AddRemoteRepositoryUseCaseImpl(
    val repositoryService: RepositoryService,
    val registry: RegistryDataStore,
    val fileStores: FileStores,
    val storageRegistry: StorageRegistry,
) : AddRemoteRepositoryUseCase {
    override suspend fun invoke(
        uri: RepositoryURI,
        credentials: BasicAuthCredentials?,
    ) {
        val result =
            repositoryService
                .getRepository(
                    uri,
                ).rawAccessor
                .dropWhile { it is ProtectedLoadableResource.Loading }
                .first()

        withContext(Dispatchers.IO) {
            when (result) {
                is ProtectedLoadableResource.Failed -> throw result.throwable
                ProtectedLoadableResource.Loading -> TODO("Shouldn't happen")
                is ProtectedLoadableResource.PendingAuthentication -> {
                    if (credentials == null) {
                        throw RequiresCredentialsException()
                    } else {
                        try {
                            result.authenticationRequest.tryOpen(
                                credentials,
                                UnlockOptions(false),
                            )
                        } catch (e: Throwable) {
                            throw WrongCredentialsException(cause = e)
                        }
                    }
                }

                is ProtectedLoadableResource.Loaded -> {
                    println("Success - result: $result")
                }
            }
        }

        repositoryService.registerRepository(repositoryURI = uri)
    }
}
