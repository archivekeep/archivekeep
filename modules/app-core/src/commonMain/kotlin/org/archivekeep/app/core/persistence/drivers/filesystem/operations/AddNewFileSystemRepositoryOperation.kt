package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.generics.Execution
import org.archivekeep.app.core.utils.generics.perform
import org.archivekeep.files.driver.filesystem.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.driver.filesystem.files.FilesRepo
import org.archivekeep.files.driver.filesystem.files.FilesSqliteRepo

class AddNewFileSystemRepositoryOperation(
    scope: CoroutineScope,
    registry: RegistryDataStore,
    fileStores: FileStores,
    path: String,
    intendedStorageType: FileSystemStorageType?,
    override val storageMarking: AddFileSystemRepositoryOperation.StorageMarking,
    override val encryptedNotPossibleDueToNotEmpty: Boolean,
) : AddFileSystemRepositoryOperationImpl(
        scope,
        registry,
        fileStores,
        path,
        intendedStorageType,
    ),
    AddFileSystemRepositoryOperation.DirectoryNotRepository {
    private val initMutableStateFlow = MutableStateFlow<Execution>(Execution.NotRunning)
    private val addMutableStateFlow = MutableStateFlow<Execution>(Execution.NotRunning)
    private val storageMarkMutableStateFlow = MutableStateFlow<Execution?>(null)

    override val initStatus = addMutableStateFlow.asStateFlow()
    override val addStatus = addMutableStateFlow.asStateFlow()
    override val storageMarkStatus = storageMarkMutableStateFlow.asStateFlow()

    override suspend fun startInitAsPlain(
        applyMarking: Boolean?,
        sqliteDB: Boolean,
    ) {
        checkMarking(storageMarking, applyMarking)

        initMutableStateFlow.perform {
            if (sqliteDB) {
                FilesSqliteRepo.create(pathPath)
            } else {
                FilesRepo.create(pathPath)
            }
        }

        runAdd(
            addMutableStateFlow,
            storageMarkMutableStateFlow,
        )
    }

    override suspend fun startInitAsEncrypted(
        applyMarking: Boolean?,
        password: String,
    ) {
        checkMarking(storageMarking, applyMarking)
        if (encryptedNotPossibleDueToNotEmpty) {
            throw IllegalStateException("Can't be initialized as encrypted, because it's not empty")
        }

        initMutableStateFlow.perform {
            EncryptedFileSystemRepository.create(pathPath, password)
        }

        runAdd(
            addMutableStateFlow,
            storageMarkMutableStateFlow,
        )
    }
}
