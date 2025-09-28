package org.archivekeep.app.ui.domain.wiring

import com.google.common.collect.ClassToInstanceMap
import com.google.common.collect.ImmutableClassToInstanceMap
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageRegistry
import org.archivekeep.app.core.operations.AddRemoteRepositoryUseCase
import org.archivekeep.app.core.operations.AddRemoteRepositoryUseCaseImpl
import org.archivekeep.app.core.operations.AssociateRepositoryOperation
import org.archivekeep.app.core.operations.AssociateRepositoryOperationImpl
import org.archivekeep.app.core.persistence.credentials.CredentialsStore
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryUseCase
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.AddFileSystemRepositoryUseCaseImpl
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.DeinitializeFileSystemRepositoryUseCase
import org.archivekeep.app.core.persistence.drivers.filesystem.operations.DeinitializeFileSystemRepositoryUseCaseImpl
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.persistence.registry.RegistryDataStore
import org.archivekeep.app.core.utils.identifiers.RepositoryURI

class OperationFactory private constructor(
    private val factories: ClassToInstanceMap<Any>,
) {
    constructor(
        repositoryService: RepositoryService,
        env: Environment,
        storageRegistry: StorageRegistry,
        drivers: Map<String, StorageDriver>,
    ) : this(
        ImmutableClassToInstanceMap
            .builder<Any>()
            .put(
                AddFileSystemRepositoryUseCase::class.java,
                AddFileSystemRepositoryUseCaseImpl(
                    env.registry,
                    env.fileStores,
                    storageRegistry,
                ),
            ).put(
                AddRemoteRepositoryUseCase::class.java,
                AddRemoteRepositoryUseCaseImpl(
                    repositoryService,
                    env.registry,
                    env.credentialsStore,
                    drivers,
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
            ).put(
                DeinitializeFileSystemRepositoryUseCase::class.java,
                DeinitializeFileSystemRepositoryUseCaseImpl(),
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
