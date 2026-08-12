package org.archivekeep.app.core.persistence.drivers.filesystem.operations.add

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
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
    override val storageRegistration: StorageRegistrationState,
) : AddFileSystemRepositoryOperationImpl(
        scope,
        registry,
        fileStores,
        path,
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
            EncryptedFileSystemRepository.openAndUnlock(pathPath, password)
        }
    }

    override suspend fun executeAdd(storageRegistrationInput: StorageRegistrationInput?) {
        checkInput(storageRegistration, storageRegistrationInput)

        unlockStatus.value.let {
            if (it !is Execution.Finished || it.outcome !is ExecutionOutcome.Success) {
                throw IllegalStateException("Not unlocked")
            }
        }

        runAdd(addMutableStateFlow, storageMarkMutableStateFlow, storageRegistrationInput)
    }
}
