package org.archivekeep.app.desktop.domain.wiring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.domain.archives.DefaultArchiveService
import org.archivekeep.app.core.domain.repositories.DefaultRepositoryService
import org.archivekeep.app.core.domain.storages.KnownStorageService
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.add.AddOperationSupervisorServiceImpl
import org.archivekeep.app.core.operations.addpush.AddAndPushOperationServiceImpl
import org.archivekeep.app.core.operations.sync.RepoToRepoSyncServiceImpl
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.drivers.filesystem.FileSystemStorageDriver
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.desktop.domain.services.RepositoryOpenService

@Composable
fun ApplicationProviders(
    scope: CoroutineScope,
    environment: Environment,
    fileStores: FileStores,
    content: @Composable () -> Unit,
) {
    val derivedServices =
        remember(
            scope,
            fileStores,
            environment,
        ) {
            object {
                val knownStorageService = KnownStorageService(environment.registry, fileStores)

                val repoService =
                    DefaultRepositoryService(
                        scope,
                        environment.storageDrivers,
                        environment.registry,
                        environment.repositoryIndexMemory,
                        environment.repositoryMetadataMemory,
                    )

                val storageService =
                    StorageService(
                        scope,
                        knownStorageService,
                        environment.storageDrivers,
                        repoService,
                    )

                val archiveService = DefaultArchiveService(scope, storageService)
                val syncService = RepoToRepoSyncServiceImpl(scope, repoService)
                val addPushService = AddAndPushOperationServiceImpl(repoService)
                val addOperationSupervisorService = AddOperationSupervisorServiceImpl(scope, repoService)

                val operationFactory =
                    OperationFactory(
                        repoService,
                        environment.registry,
                        fileStores,
                        knownStorageService,
                    )

                val repositoryOpenService =
                    RepositoryOpenService(
                        environment.storageDrivers.values
                            .filterIsInstance<FileSystemStorageDriver>()
                            .firstOrNull(),
                    )
            }
        }

    CompositionLocalProvider(
        LocalArchiveService provides derivedServices.archiveService,
        LocalStorageService provides derivedServices.storageService,
        LocalWalletDataStore provides environment.walletDataStore,
        LocalRepoService provides derivedServices.repoService,
        LocalRepoToRepoSyncService provides derivedServices.syncService,
        LocalAddPushService provides derivedServices.addPushService,
        LocalAddOperationSupervisorService provides derivedServices.addOperationSupervisorService,
        LocalRegistry provides environment.registry,
        LocalFileStores provides fileStores,
        LocalStorageRegistry provides derivedServices.knownStorageService,
        LocalOperationFactory provides derivedServices.operationFactory,
        LocalRepositoryOpenService provides derivedServices.repositoryOpenService,
    ) {
        content()
    }
}
