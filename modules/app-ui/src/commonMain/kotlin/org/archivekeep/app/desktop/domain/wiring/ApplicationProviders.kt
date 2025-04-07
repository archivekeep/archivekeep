package org.archivekeep.app.desktop.domain.wiring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.plus
import org.archivekeep.app.core.domain.archives.DefaultArchiveService
import org.archivekeep.app.core.domain.repositories.DefaultRepositoryService
import org.archivekeep.app.core.domain.storages.KnownStorageService
import org.archivekeep.app.core.domain.storages.StorageService
import org.archivekeep.app.core.operations.add.AddOperationSupervisorServiceImpl
import org.archivekeep.app.core.operations.addpush.AddAndPushOperationServiceImpl
import org.archivekeep.app.core.operations.sync.RepoToRepoSyncServiceImpl
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.desktop.domain.services.LocalSharingCoroutineDispatcher
import org.archivekeep.app.desktop.domain.services.createRepositoryOpenService

@OptIn(DelicateCoroutinesApi::class)
class ApplicationServices(
    basescope: CoroutineScope,
    environmentFactory: (scope: CoroutineScope) -> Environment,
) {
    val executor = newFixedThreadPoolContext(16, "Application Services")
    val scope = basescope + executor

    val environment = environmentFactory(scope)

    val knownStorageService = KnownStorageService(environment.registry, environment.fileStores)

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
    val addPushService = AddAndPushOperationServiceImpl(scope, repoService)
    val addOperationSupervisorService = AddOperationSupervisorServiceImpl(scope, repoService)

    val operationFactory =
        OperationFactory(
            repoService,
            environment.registry,
            environment.fileStores,
            knownStorageService,
        )

    fun close() {
        executor.close()
    }
}

@Composable
fun ApplicationProviders(
    environmentFactory: (scope: CoroutineScope) -> Environment,
    content: @Composable () -> Unit,
) {
    val basescope = rememberCoroutineScope()

    val applicationServicesRemember =
        remember(basescope, environmentFactory) {
            object : RememberObserver {
                val services = ApplicationServices(basescope, environmentFactory)

                override fun onAbandoned() {
                    services.close()
                }

                override fun onForgotten() {
                    services.close()
                }

                override fun onRemembered() = Unit
            }
        }

    ApplicationProviders(applicationServicesRemember.services, content)
}

@Composable
fun ApplicationProviders(
    applicationServices: ApplicationServices,
    content: @Composable () -> Unit,
) {
    val repositoryOpenService =
        remember(applicationServices) {
            createRepositoryOpenService(applicationServices)
        }

    CompositionLocalProvider(
        LocalArchiveService provides applicationServices.archiveService,
        LocalStorageService provides applicationServices.storageService,
        LocalWalletDataStore provides applicationServices.environment.walletDataStore,
        LocalRepoService provides applicationServices.repoService,
        LocalRepoToRepoSyncService provides applicationServices.syncService,
        LocalAddPushService provides applicationServices.addPushService,
        LocalAddOperationSupervisorService provides applicationServices.addOperationSupervisorService,
        LocalRegistry provides applicationServices.environment.registry,
        LocalFileStores provides applicationServices.environment.fileStores,
        LocalStorageRegistry provides applicationServices.knownStorageService,
        LocalOperationFactory provides applicationServices.operationFactory,
        LocalRepositoryOpenService provides repositoryOpenService,
        LocalSharingCoroutineDispatcher provides applicationServices.executor,
    ) {
        content()
    }
}
