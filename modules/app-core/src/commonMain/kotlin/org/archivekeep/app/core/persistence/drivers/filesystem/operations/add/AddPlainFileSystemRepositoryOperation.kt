package org.archivekeep.app.core.persistence.drivers.filesystem.operations.add

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.generics.Execution

class AddPlainFileSystemRepositoryOperation(
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
    AddFileSystemRepositoryOperation.PlainFileSystemRepository {
    private val addMutableStateFlow = MutableStateFlow<Execution>(Execution.NotRunning)
    private val storageMarkMutableStateFlow = MutableStateFlow<Execution?>(null)

    override val addStatus = addMutableStateFlow.asStateFlow()
    override val storageMarkStatus = storageMarkMutableStateFlow.asStateFlow()

    override suspend fun executeAdd(storageRegistrationInput: StorageRegistrationInput?) {
        checkInput(storageRegistration, storageRegistrationInput)

        runAdd(addMutableStateFlow, storageMarkMutableStateFlow, storageRegistrationInput)
    }
}
