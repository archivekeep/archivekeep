package org.archivekeep.app.core.persistence.drivers.filesystem.operations.add

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.generics.Execution
import org.archivekeep.app.core.utils.generics.perform
import org.archivekeep.files.driver.filesystem.encryptedfiles.EncryptedFileSystemRepository
import org.archivekeep.files.driver.filesystem.files.FilesRepo
import org.archivekeep.files.driver.filesystem.files.FilesSqliteRepo
import org.archivekeep.files.driver.filesystem.files.objectbox.FilesObjectBoxRepo

class AddNewFileSystemRepositoryOperation(
    scope: CoroutineScope,
    registry: RegistryDataStore,
    fileStores: FileStores,
    path: String,
    override val storageRegistration: StorageRegistrationState,
    override val encryptedNotPossibleDueToNotEmpty: Boolean,
) : AddFileSystemRepositoryOperationImpl(
        scope,
        registry,
        fileStores,
        path,
    ),
    AddFileSystemRepositoryOperation.DirectoryNotRepository {
    private val initMutableStateFlow = MutableStateFlow<Execution>(Execution.NotRunning)
    private val addMutableStateFlow = MutableStateFlow<Execution>(Execution.NotRunning)
    private val storageMarkMutableStateFlow = MutableStateFlow<Execution?>(null)

    override val initStatus = addMutableStateFlow.asStateFlow()
    override val addStatus = addMutableStateFlow.asStateFlow()
    override val storageMarkStatus = storageMarkMutableStateFlow.asStateFlow()

    override suspend fun startInitAsPlain(
        storageRegistrationInput: StorageRegistrationInput?,
        repoType: PlainRepositoryType,
    ) {
        checkInput(storageRegistration, storageRegistrationInput)

        initMutableStateFlow.perform {
            when (repoType) {
                PlainRepositoryType.OBJECTBOX -> FilesObjectBoxRepo.create(pathPath).also { it.close() }
                PlainRepositoryType.SQLITE -> FilesSqliteRepo.create(pathPath)
                PlainRepositoryType.CHECKSUM_FILES -> FilesRepo.create(pathPath)
            }
        }

        runAdd(addMutableStateFlow, storageMarkMutableStateFlow, storageRegistrationInput)
    }

    override suspend fun startInitAsEncrypted(
        storageRegistrationInput: StorageRegistrationInput?,
        password: String,
    ) {
        checkInput(storageRegistration, storageRegistrationInput)

        if (encryptedNotPossibleDueToNotEmpty) {
            throw IllegalStateException("Can't be initialized as encrypted, because it's not empty")
        }

        initMutableStateFlow.perform {
            EncryptedFileSystemRepository.create(pathPath, password)
        }

        runAdd(addMutableStateFlow, storageMarkMutableStateFlow, storageRegistrationInput)
    }
}
