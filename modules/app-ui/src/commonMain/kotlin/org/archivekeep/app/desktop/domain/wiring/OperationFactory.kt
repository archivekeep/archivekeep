package org.archivekeep.app.desktop.domain.wiring

import com.google.common.collect.ClassToInstanceMap
import com.google.common.collect.ImmutableClassToInstanceMap
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.operations.AddRemoteRepositoryOperation
import org.archivekeep.app.core.operations.AddRemoteRepositoryOperationImpl
import org.archivekeep.app.core.operations.AssociateRepositoryOperation
import org.archivekeep.app.core.operations.AssociateRepositoryOperationImpl
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageType
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperation
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryOperationImpl
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI
import org.archivekeep.files.repo.remote.grpc.BasicAuthCredentials

class OperationFactory(
    val repositoryService: RepositoryService,
    val registry: RegistryDataStore,
    val fileStores: FileStores,
    val storageRegistry: StorageRegistry,
) {
    val factories: ClassToInstanceMap<Any>

    init {
        factories =
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
                    AddRemoteRepositoryOperation.Factory::class.java,
                    object : AddRemoteRepositoryOperation.Factory {
                        override fun create(
                            scope: CoroutineScope,
                            url: String,
                            credentials: BasicAuthCredentials?,
                        ): AddRemoteRepositoryOperation =
                            AddRemoteRepositoryOperationImpl(
                                scope,
                                repositoryService,
                                registry,
                                fileStores,
                                storageRegistry,
                                url,
                                credentials,
                            )
                    },
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
                ).build()
    }

    fun <T : Any> get(clazz: Class<T>): T = factories.getInstance(clazz) ?: throw RuntimeException("Class $clazz not supported")
}
