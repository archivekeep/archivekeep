package org.archivekeep.app.core.persistence.drivers.filesystem.operations.add

import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType

data class StorageRegistrationInput(
    val storageType: FileSystemStorageType,
    val label: String,
)
