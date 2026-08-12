package org.archivekeep.app.core.persistence.drivers.filesystem.operations.add

import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType

sealed interface StorageRegistrationState {
    data class Registered(
        val isLocal: Boolean,
        val label: String,
    ) : StorageRegistrationState

    data class Unregistered(
        val suggestedType: FileSystemStorageType?,
        val suggestedLabel: String?,
    ) : StorageRegistrationState
}
