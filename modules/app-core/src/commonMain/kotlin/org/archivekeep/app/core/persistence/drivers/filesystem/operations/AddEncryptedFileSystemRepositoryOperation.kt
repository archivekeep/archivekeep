package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.generics.Execution
import org.archivekeep.app.core.utils.generics.ExecutionOutcome
import org.archivekeep.app.core.utils.generics.perform
import org.archivekeep.files.driver.filesystem.encryptedfiles.EncryptedFileSystemRepository

class AddEncryptedFileSystemRepositoryOperation(
    scope: CoroutineScope,
    registry: RegistryDataStore,
    fileStores: FileStores,
    path: String,
    intendedStorageType: FileSystemStorageType?,
    override val storageMarking: AddFileSystemRepositoryOperation.StorageMarking,
) : AddFileSystemRepositoryOperationImpl(
        scope,
        registry,
        fileStores,
        path,
        intendedStorageType,
    ),
    AddFileSystemRepositoryOperation.EncryptedFileSystemRepository {
    private val unlockMutableStateFlow = MutableStateFlow<Execution>(Execution.NotRunning)
    private val addMutableStateFlow = MutableStateFlow<Execution>(Execution.NotRunning)
    private val storageMarkMutableStateFlow = MutableStateFlow<Execution?>(null)

    override val unlockStatus = unlockMutableStateFlow.asStateFlow()
    override val addStatus = addMutableStateFlow.asStateFlow()
    override val storageMarkStatus = storageMarkMutableStateFlow.asStateFlow()

    override suspend fun unlock(password: String) {
        unlockMutableStateFlow.perform {
            EncryptedFileSystemRepository.Companion.openAndUnlock(pathPath, password)
        }
    }

    override suspend fun runAddExecution(applyMarking: Boolean?) {
        checkMarking(storageMarking, applyMarking)

        unlockStatus.value.let {
            if (it !is Execution.Finished || it.outcome !is ExecutionOutcome.Success) {
                throw IllegalStateException("Not unlocked")
            }
        }

        runAdd(addMutableStateFlow, storageMarkMutableStateFlow)
    }
}
