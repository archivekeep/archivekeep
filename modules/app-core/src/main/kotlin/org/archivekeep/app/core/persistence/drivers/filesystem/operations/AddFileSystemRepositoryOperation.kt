package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType

interface AddFileSystemRepositoryOperation {
    val preparationStatus: StateFlow<PreparationStatus>
    val initStatus: StateFlow<InitStatus?>
    val addStatus: StateFlow<AddStatus?>
    val storageMarkStatus: StateFlow<StorageMarkStatus?>
    val completed: StateFlow<Boolean>

    interface Factory {
        fun create(
            scope: CoroutineScope,
            path: String,
            intendedStorageType: FileSystemStorageType?,
        ): AddFileSystemRepositoryOperation
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

    sealed interface PreparationStatus {
        sealed interface PreparationNoContinue : PreparationStatus

        data object Preparing : PreparationStatus

        data class PreparationException(
            val cause: Throwable,
        ) : PreparationNoContinue

        data object AlreadyRegistered :
            PreparationNoContinue

        data class NotRoot(
            val rootPath: String,
        ) : PreparationNoContinue

        data object NotExisting :
            PreparationNoContinue

        data class ReadyForAdd(
            // TODO: implementation detail between state update and UI block
            val startAddExecution: (applyMarking: Boolean?) -> Unit,
            val storageMarking: StorageMarking,
        ) : PreparationStatus

        data class ReadyForInit(
            // TODO: implementation detail between state update and UI block
            val startInit: (applyMarking: Boolean?) -> Unit,
            val storageMarking: StorageMarking,
        ) : PreparationStatus
    }

    sealed interface InitStatus {
        data object Initializing : InitStatus

        data object InitSuccessful : InitStatus

        data class InitFailed(
            val reason: String,
            val cause: Throwable?,
        ) : InitStatus
    }

    sealed interface AddStatus {
        data object Adding : AddStatus

        data object AddSuccessful : AddStatus

        data class AddFailed(
            val reason: String,
            val cause: Throwable?,
        ) : AddStatus
    }

    sealed interface StorageMarkStatus {
        data object Marking : StorageMarkStatus

        data object Successful : StorageMarkStatus

        data class Failed(
            val reason: String,
            val cause: Throwable?,
        ) : StorageMarkStatus
    }

    fun cancel()
}
