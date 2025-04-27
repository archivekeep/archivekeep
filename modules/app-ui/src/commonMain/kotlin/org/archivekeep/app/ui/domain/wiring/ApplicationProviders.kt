package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.plus
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.ui.domain.services.LocalSharingCoroutineDispatcher
import org.archivekeep.app.ui.domain.services.createRepositoryOpenService

@Composable
fun ApplicationProviders(
    environmentFactory: (scope: CoroutineScope) -> Environment,
    content: @Composable () -> Unit,
) {
    val basescope = rememberCoroutineScope()

    val applicationServicesRemember =
        remember(basescope, environmentFactory) {
            object : RememberObserver {
                val job = SupervisorJob(basescope.coroutineContext.job)
                val scope = CoroutineScope(job)
                val serviceWorkDispatcher = newServiceWorkExecutorDispatcher()
                val environment = environmentFactory(scope + serviceWorkDispatcher)
                val services = ApplicationServices(serviceWorkDispatcher, scope, environment)

                override fun onAbandoned() {
                    job.cancel()
                    serviceWorkDispatcher.close()
                }

                override fun onForgotten() {
                    job.cancel()
                    serviceWorkDispatcher.close()
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
        LocalSharingCoroutineDispatcher provides applicationServices.serviceWorkDispatcher,
    ) {
        content()
    }
}
