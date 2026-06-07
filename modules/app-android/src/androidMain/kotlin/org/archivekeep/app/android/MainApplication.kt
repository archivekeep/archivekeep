package org.archivekeep.app.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import org.archivekeep.app.ui.domain.wiring.ApplicationServicesGraph
import org.archivekeep.app.ui.domain.wiring.createApplicationServices
import org.archivekeep.app.ui.domain.wiring.newServiceWorkExecutorDispatcher

class MainApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob())
    private val serviceWorkDispatcher: ExecutorCoroutineDispatcher = newServiceWorkExecutorDispatcher()

    lateinit var services: ApplicationServicesGraph

    override fun onCreate() {
        super.onCreate()

        val environment =
            AndroidEnvironment(
                applicationContext,
                serviceWorkDispatcher.executor,
                scope + serviceWorkDispatcher,
                AndroidEnvironmentPaths(filesDir),
            )

        services = createApplicationServices(serviceWorkDispatcher, scope, environment)
    }
}
