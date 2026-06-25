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
import org.archivekeep.app.ui.domain.services.createRepositoryOpenService

@Composable
fun ApplicationProviders(
    applicationServicesFactory: (scope: CoroutineScope, serviceWorkDispatcher: CoroutineDispatcher) -> ApplicationServices,
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
        LocalRepositoryOpenService provides repositoryOpenService,
        LocalApplicationServices provides applicationServices,
    ) {
        content()
    }
}
