package org.archivekeep.app.core.persistence.drivers.filesystem.operations.deinitialize

sealed interface DeinitializeFileSystemRepositoryPreparation {
    data object DirectoryNotRepository : DeinitializeFileSystemRepositoryPreparation

    interface PlainFileSystemRepository : DeinitializeFileSystemRepositoryPreparation {
        suspend fun runDeinitialize()
    }
}
