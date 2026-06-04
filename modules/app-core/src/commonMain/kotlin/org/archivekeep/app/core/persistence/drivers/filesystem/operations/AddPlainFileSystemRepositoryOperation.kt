package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.generics.Execution

class AddPlainFileSystemRepositoryOperation(
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
    AddFileSystemRepositoryOperation.PlainFileSystemRepository {
    private val addMutableStateFlow = MutableStateFlow<Execution>(Execution.NotRunning)
    private val storageMarkMutableStateFlow = MutableStateFlow<Execution?>(null)

    override val addStatus = addMutableStateFlow.asStateFlow()
    override val storageMarkStatus = storageMarkMutableStateFlow.asStateFlow()

    override suspend fun runAddExecution(applyMarking: Boolean?) {
        checkMarking(storageMarking, applyMarking)

        runAdd(addMutableStateFlow, storageMarkMutableStateFlow)
    }
}
