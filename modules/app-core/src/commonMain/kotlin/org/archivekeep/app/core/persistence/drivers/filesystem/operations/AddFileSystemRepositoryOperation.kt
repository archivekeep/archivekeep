package org.archivekeep.app.core.persistence.drivers.filesystem.operations

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

    interface PlainFileSystemRepository : AddFileSystemRepositoryOperation {
        val storageMarking: StorageMarking

        val addStatus: StateFlow<Execution>
        val storageMarkStatus: StateFlow<Execution?>

        suspend fun runAddExecution(applyMarking: Boolean?)
    }

    interface EncryptedFileSystemRepository : AddFileSystemRepositoryOperation {
        val storageMarking: StorageMarking

        val unlockStatus: StateFlow<Execution>
        val addStatus: StateFlow<Execution>
        val storageMarkStatus: StateFlow<Execution?>

        suspend fun unlock(password: String)

        suspend fun runAddExecution(applyMarking: Boolean?)
    }

    interface DirectoryNotRepository : AddFileSystemRepositoryOperation {
        val storageMarking: StorageMarking

        val initStatus: StateFlow<Execution>
        val addStatus: StateFlow<Execution>
        val storageMarkStatus: StateFlow<Execution?>

        suspend fun startInitAsPlain(applyMarking: Boolean?)

        suspend fun startInitAsEncrypted(
            applyMarking: Boolean?,
            password: String,
        )
    }

    enum class StorageMarking(
        val isMark: Boolean = false,
        val isRemark: Boolean = false,
    ) {
        ALRIGHT,
        NEEDS_MARK_AS_LOCAL(true),
        NEEDS_MARK_AS_EXTERNAL(true),
        NEEDS_REMARK_AS_LOCAL(false, true),
        NEEDS_REMARK_AS_EXTERNAL(false, true),
    }
}
