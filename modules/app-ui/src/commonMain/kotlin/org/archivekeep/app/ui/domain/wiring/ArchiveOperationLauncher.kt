package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.staticCompositionLocalOf
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.app.core.utils.identifiers.StorageURI

data class ArchiveOperationLaunchers(
    val openAddAndPushOperation: (repositoryURI: RepositoryURI) -> Unit,
    val openIndexUpdateOperation: (repositoryURI: RepositoryURI) -> Unit,
    val openAssociateRepository: (repositoryURI: RepositoryURI) -> Unit,
    val openUnassociateRepository: (repositoryURI: RepositoryURI) -> Unit,
    val openForgetRepository: (repositoryURI: RepositoryURI) -> Unit,
    val openDeinitializeFilesystemRepository: (path: String) -> Unit,
    val unlockRepository: (repositoryURI: RepositoryURI, onUnlock: (() -> Unit)?) -> Unit,
    val pushRepoToAll: (repositoryURI: RepositoryURI) -> Unit,
    val openAddFileSystemRepository: (intendedFileSystemStorageType: FileSystemStorageType?) -> Unit,
    val openAddRemoteRepository: () -> Unit,
    val pushAllToStorage: (storageURI: StorageURI) -> Unit,
    val pullAllFromStorage: (storageURI: StorageURI) -> Unit,
    val pushToRepo: (repositoryURI: RepositoryURI, from: RepositoryURI) -> Unit,
    val pullFromRepo: (from: RepositoryURI, to: RepositoryURI) -> Unit,
)

val LocalArchiveOperationLaunchers =
    staticCompositionLocalOf {
        ArchiveOperationLaunchers(
            openAddAndPushOperation = { invalidUseOfContext("openAddAndPushOperation") },
            openIndexUpdateOperation = { invalidUseOfContext("openIndexUpdateOperation") },
            openAssociateRepository = { invalidUseOfContext("openAssociateRepository") },
            openUnassociateRepository = { invalidUseOfContext("openUnassociateRepository") },
            openForgetRepository = { invalidUseOfContext("openForgetRepository") },
            openDeinitializeFilesystemRepository = { invalidUseOfContext("openDeinitializeFilesystemRepository") },
            unlockRepository = { _, _ -> invalidUseOfContext("unlockRepository") },
            pushRepoToAll = { invalidUseOfContext("pushRepoToAll") },
            openAddFileSystemRepository = { invalidUseOfContext("openAddFileSystemRepository") },
            openAddRemoteRepository = { invalidUseOfContext("openAddRemoteRepository") },
            pushAllToStorage = { invalidUseOfContext("pushAllToStorage") },
            pullAllFromStorage = { invalidUseOfContext("pullAllFromStorage") },
            pushToRepo = { _, _ -> invalidUseOfContext("pushToRepo") },
            pullFromRepo = { _, _ -> invalidUseOfContext("pullFromRepo") },
        )
    }

private fun invalidUseOfContext(name: String): Nothing = throw Error("Context must be present to call $name")
