package org.archivekeep.app.ui.domain.wiring

import com.google.common.collect.ClassToInstanceMap
import com.google.common.collect.ImmutableClassToInstanceMap
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.operations.AddRemoteRepositoryUseCase
import org.archivekeep.app.core.operations.AddRemoteRepositoryUseCaseImpl
import org.archivekeep.app.core.operations.AssociateRepositoryOperation
import org.archivekeep.app.core.operations.AssociateRepositoryOperationImpl
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperationImpl
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI

class OperationFactory private constructor(
    private val factories: ClassToInstanceMap<Any>,
) {
    constructor(
        repositoryService: RepositoryService,
        registry: RegistryDataStore,
        fileStores: FileStores,
        storageRegistry: StorageRegistry,
    ) : this(
        ImmutableClassToInstanceMap
            .builder<Any>()
            .put(
                AddFileSystemRepositoryOperation.Factory::class.java,
                object : AddFileSystemRepositoryOperation.Factory {
                    override fun create(
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
                },
            ).put(
                AddRemoteRepositoryUseCase::class.java,
                AddRemoteRepositoryUseCaseImpl(
                    repositoryService,
                    registry,
                    fileStores,
                    storageRegistry,
                ),
            ).put(
                AssociateRepositoryOperation.Factory::class.java,
                object : AssociateRepositoryOperation.Factory {
                    override fun create(
                        scope: CoroutineScope,
                        uri: RepositoryURI,
                    ): AssociateRepositoryOperation =
                        AssociateRepositoryOperationImpl(
                            scope,
                            repositoryService,
                            uri,
                        )
                },
            ).build(),
    )

    fun <T : Any> override(
        key: Class<T>,
        value: T,
    ) = OperationFactory(
        ImmutableClassToInstanceMap
            .builder<Any>()
            .putAll(this.factories.filterKeys { it != key })
            .put(key, value)
            .build(),
    )

    fun <T : Any> get(clazz: Class<T>): T = factories.getInstance(clazz) ?: throw RuntimeException("Class $clazz not supported")
}
