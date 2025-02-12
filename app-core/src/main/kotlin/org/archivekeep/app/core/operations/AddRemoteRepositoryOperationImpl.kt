package org.archivekeep.app.core.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.repositories.UnlockOptions
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.operations.AddRemoteRepositoryOperation.AddStatus
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.ProtectedLoadableResource
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

class AddRemoteRepositoryOperationImpl(
    val scope: CoroutineScope,
    val repositoryService: RepositoryService,
    val registry: RegistryDataStore,
    val fileStores: FileStores,
    val storageRegistry: StorageRegistry,
    val url: String,
    val basicAuthCredentials: BasicAuthCredentials?,
) : AddRemoteRepositoryOperation {
    private val addMutableStateFlow: MutableStateFlow<AddStatus> = MutableStateFlow(AddStatus.Adding)
    private val completedMutableStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override val addStatus = addMutableStateFlow.asStateFlow()
    override val completed = completedMutableStateFlow.asStateFlow()

    init {
        launchAdd()
    }

    private var launchedAdd: Job? = null

    override fun cancel() {
        launchedAdd?.cancel()
    }

    private fun launchAdd() {
        if (launchedAdd != null) {
            throw RuntimeException("Already launched")
        }

        addMutableStateFlow.value = AddStatus.Adding

        launchedAdd =
            scope.launch {
                try {
                    val uri = RepositoryURI.fromFull(url.trim())
                    val result =
                        repositoryService
                            .getRepository(
                                uri,
                            ).rawAccessor
                            .dropWhile { it is ProtectedLoadableResource.Loading }
                            .first()

                    when (result) {
                        is ProtectedLoadableResource.Failed -> throw result.throwable
                        ProtectedLoadableResource.Loading -> TODO("Shouldn't happen")
                        is ProtectedLoadableResource.PendingAuthentication -> {
                            if (basicAuthCredentials == null) {
                                addMutableStateFlow.value = AddStatus.RequiresCredentials
                                return@launch
                            } else {
                                try {
                                    result.authenticationRequest.tryOpen(
                                        basicAuthCredentials,
                                        UnlockOptions(false),
                                    )
                                } catch (e: Throwable) {
                                    addMutableStateFlow.value = AddStatus.WrongCredentials
                                    return@launch
                                }
                            }
                        }
                        is ProtectedLoadableResource.Loaded -> {
                            println("Success - result: $result")
                        }
                    }

                    repositoryService.registerRepository(repositoryURI = uri)
                    addMutableStateFlow.value = (AddStatus.AddSuccessful)
                } catch (e: Throwable) {
                    addMutableStateFlow.value = (
                        AddStatus.AddFailed(
                            e.message ?: e.toString(),
                            e,
                        )
                    )
                } finally {
                    completedMutableStateFlow.value = true
                }
            }
    }
}
