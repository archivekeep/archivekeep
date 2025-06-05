package org.archivekeep.app.core.persistence.drivers.filesystem.operations

import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.registry.RegistryDataStore

class AddFileSystemRepositoryUseCaseImpl(
    val registry: RegistryDataStore,
    val fileStores: FileStores,
    val storageRegistry: StorageRegistry,
) : AddFileSystemRepositoryUseCase {
    override fun begin(
        scope: CoroutineScope,
        path: String,
        intendedStorageType: FileSystemStorageType?,
    ): AddFileSystemRepositoryOperation =
        AddFileSystemRepositoryOperationImpl(
            scope,
            registry,
            fileStores,
            storageRegistry,
            path,
            intendedStorageType,
        )
}
