package org.archivekeep.app.ui.domain.wiring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.plus
import org.archivekeep.app.ui.domain.services.LocalSharingCoroutineDispatcher
import org.archivekeep.app.ui.domain.services.createRepositoryOpenService
import org.archivekeep.app.ui.utils.ApplicationMetadata
import org.archivekeep.app.ui.utils.LocalApplicationMetadata

@Composable
fun ApplicationProviders(
    applicationServicesFactory: (scope: CoroutineScope, serviceWorkDispatcher: CoroutineDispatcher) -> ApplicationServices,
    applicationMetadata: ApplicationMetadata,
    content: @Composable () -> Unit,
) {
    val basescope = rememberCoroutineScope()

    val applicationServicesRemember =
        remember(basescope, applicationServicesFactory) {
            object : RememberObserver {
                val job = SupervisorJob(basescope.coroutineContext.job)
                val scope = CoroutineScope(job)
                val serviceWorkDispatcher = newServiceWorkExecutorDispatcher()
                val services = applicationServicesFactory(scope + serviceWorkDispatcher, serviceWorkDispatcher)

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

    ApplicationProviders(applicationServicesRemember.services, applicationMetadata, content)
}

@Composable
fun ApplicationProviders(
    applicationServices: ApplicationServices,
    applicationMetadata: ApplicationMetadata,
    content: @Composable () -> Unit,
) {
    val repositoryOpenService =
        remember(applicationServices) {
            createRepositoryOpenService(applicationServices)
        }

    CompositionLocalProvider(
        LocalApplicationMetadata provides applicationMetadata,
        LocalArchiveService provides applicationServices.archiveService,
        LocalStorageService provides applicationServices.storageService,
        LocalRepoService provides applicationServices.repositoryService,
        LocalRepoToRepoSyncService provides applicationServices.syncService,
        LocalAddPushService provides applicationServices.addPushService,
        LocalIndexUpdateProcedureSupervisorService provides applicationServices.addOperationSupervisorService,
        LocalRegistry provides applicationServices.registry,
        LocalFileStores provides applicationServices.fileStores,
        LocalOperationFactory provides applicationServices.operationFactory,
        LocalRepositoryOpenService provides repositoryOpenService,
        LocalSharingCoroutineDispatcher provides applicationServices.serviceWorkDispatcher,
        LocalApplicationServices provides applicationServices,
    ) {
        content()
    }
}
