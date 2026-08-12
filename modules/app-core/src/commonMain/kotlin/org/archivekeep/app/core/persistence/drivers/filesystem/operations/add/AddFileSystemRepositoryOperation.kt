package org.archivekeep.app.core.persistence.drivers.filesystem.operations.add

import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.utils.generics.Execution

sealed interface AddFileSystemRepositoryOperation {
    sealed interface Invalid : AddFileSystemRepositoryOperation {
        data object AlreadyRegistered : Invalid

        data class NotRoot(
            val rootPath: String,
        ) : Invalid

        data object NotExisting : Invalid
    }

    sealed interface Available : AddFileSystemRepositoryOperation {
        val storageRegistration: StorageRegistrationState
    }

    interface PlainFileSystemRepository :
        AddFileSystemRepositoryOperation,
        Available {
        val addStatus: StateFlow<Execution>
        val storageMarkStatus: StateFlow<Execution?>

        suspend fun executeAdd(storageRegistrationInput: StorageRegistrationInput?)
    }

    interface EncryptedFileSystemRepository :
        AddFileSystemRepositoryOperation,
        Available {
        val unlockStatus: StateFlow<Execution>
        val addStatus: StateFlow<Execution>
        val storageMarkStatus: StateFlow<Execution?>

        suspend fun unlock(password: String)

        suspend fun executeAdd(storageRegistrationInput: StorageRegistrationInput?)
    }

    interface DirectoryNotRepository :
        AddFileSystemRepositoryOperation,
        Available {
        val encryptedNotPossibleDueToNotEmpty: Boolean

        val initStatus: StateFlow<Execution>
        val addStatus: StateFlow<Execution>
        val storageMarkStatus: StateFlow<Execution?>

        suspend fun startInitAsPlain(
            storageRegistrationInput: StorageRegistrationInput?,
            sqliteDB: Boolean = true,
        )

        suspend fun startInitAsEncrypted(
            storageRegistrationInput: StorageRegistrationInput?,
            password: String,
        )
    }
}
