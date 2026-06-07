package org.archivekeep.app.ui.domain.wiring

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Includes
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.archives.ArchiveService
import org.archivekeep.app.core.domain.repositories.RepositoryService
import org.archivekeep.app.core.domain.storages.StorageDriver
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.persistence.drivers.grpc.GRPCStorageDriver
import org.archivekeep.app.core.persistence.drivers.s3.S3StorageDriver
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.procedures.add.IndexUpdateProcedureSupervisorService
import org.archivekeep.app.core.procedures.addpush.AddAndPushProcedureService
import org.archivekeep.app.core.procedures.sync.RepoToRepoSyncService

@DependencyGraph(AppScope::class)
interface ApplicationServicesGraph {
    val environment: Environment
    val storageDrivers: Map<String, StorageDriver>

    val serviceWorkDispatcher: CoroutineDispatcher

    val archiveService: ArchiveService
    val storageService: StorageService
    val repositoryService: RepositoryService

    val syncService: RepoToRepoSyncService
    val addPushService: AddAndPushProcedureService
    val addOperationSupervisorService: IndexUpdateProcedureSupervisorService

    val operationFactory: OperationFactory

    @Provides
    fun provideStorageDrivers(
        scope: CoroutineScope,
        environment: Environment,
    ): Map<String, StorageDriver> =
        (
            listOf(
                GRPCStorageDriver(scope, environment.credentialsStore),
                S3StorageDriver(scope, environment.credentialsStore),
            ) + environment.storageDrivers
        ).associateBy { it.ID }

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides scope: CoroutineScope,
            @Provides serviceWorkDispatcher: CoroutineDispatcher,
            @Includes environment: Environment,
        ): ApplicationServicesGraph
    }
}
