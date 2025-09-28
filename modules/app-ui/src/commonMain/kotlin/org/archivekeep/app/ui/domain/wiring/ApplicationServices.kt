package org.archivekeep.app.ui.domain.wiring

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import org.archivekeep.app.core.domain.archives.DefaultArchiveService
import org.archivekeep.app.core.domain.repositories.DefaultRepositoryService
import org.archivekeep.app.core.domain.storages.KnownStorageService
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.persistence.drivers.grpc.GRPCStorageDriver
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.procedures.add.IndexUpdateProcedureSupervisorServiceImpl
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedureServiceImpl
import org.archivekeep.app.core.procedures.sync.RepoToRepoSyncServiceImpl

class ApplicationServices(
    val serviceWorkDispatcher: CoroutineDispatcher,
    basescope: CoroutineScope,
    val environment: Environment,
) {
    val scope = basescope + serviceWorkDispatcher

    val storageDrivers =
        (
            listOf(
                GRPCStorageDriver(scope, environment.credentialsStore),
                S3StorageDriver(scope, environment.credentialsStore),
            ) + environment.storageDrivers
        ).associateBy { it.ID }

    val knownStorageService = KnownStorageService(scope, environment.registry, environment.fileStores)

    val repoService =
        DefaultRepositoryService(
            scope,
            storageDrivers,
            environment.credentialsStore,
            environment.registry,
            environment.repositoryIndexMemory,
            environment.repositoryMetadataMemory,
        )

    val storageService =
        StorageService(
            scope,
            knownStorageService,
            storageDrivers,
            repoService,
        )

    val archiveService = DefaultArchiveService(scope, storageService)
    val syncService = RepoToRepoSyncServiceImpl(scope, repoService)
    val addPushService = AddAndPushProcedureServiceImpl(scope, repoService)
    val addOperationSupervisorService = IndexUpdateProcedureSupervisorServiceImpl(scope, repoService)

    val operationFactory =
        OperationFactory(
            repoService,
            environment,
            knownStorageService,
            storageDrivers,
        )
}
