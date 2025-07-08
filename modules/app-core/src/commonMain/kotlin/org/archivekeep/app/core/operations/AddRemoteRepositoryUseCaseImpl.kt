package org.archivekeep.app.core.operations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.archivekeep.app.core.domain.repositories.RepoAuthRequest
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.NeedsUnlock
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.generics.OptionalLoadable
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
        rememberCredentials: Boolean,
    ) {
        val repository = repositoryService.getRepository(uri)

        val result =
            repository
                .optionalAccessorFlow
                .dropWhile { it is OptionalLoadable.Loading }
                .first()

        withContext(Dispatchers.IO) {
            when (result) {
                is OptionalLoadable.Failed -> throw result.cause
                OptionalLoadable.Loading -> TODO("Shouldn't happen")
                is NeedsUnlock -> {
                    if (credentials == null) {
                        throw RequiresCredentialsException()
                    } else {
                        try {
                            (result.unlockRequest as RepoAuthRequest).tryOpen(
                                credentials,
                                UnlockOptions(rememberCredentials),
                            )
                        } catch (e: Throwable) {
                            throw WrongCredentialsException(cause = e)
                        }
                    }
                }
                is OptionalLoadable.NotAvailable -> {
                    throw RuntimeException("Not available: ${result.javaClass.name}", result.cause)
                }

                is OptionalLoadable.LoadedAvailable -> {
                    println("Success - result: $result")
                }
            }
        }

        repositoryService.registerRepository(repositoryURI = uri)
    }
}
