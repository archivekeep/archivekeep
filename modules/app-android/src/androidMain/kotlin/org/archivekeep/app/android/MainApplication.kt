package org.archivekeep.app.android

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.domain.wiring.createApplicationServices
import org.archivekeep.app.ui.domain.wiring.newServiceWorkExecutorDispatcher

class MainApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob())
    private val serviceWorkDispatcher: ExecutorCoroutineDispatcher = newServiceWorkExecutorDispatcher()

    lateinit var services: ApplicationServices

    override fun onCreate() {
        super.onCreate()

        val androidApplicationServices =
            createGraphFactory<AndroidApplicationServices.Factory>().create(
                applicationContext,
                serviceWorkDispatcher.executor,
                serviceWorkDispatcher,
                scope + serviceWorkDispatcher,
                AndroidEnvironmentPaths(filesDir),
            )

        services = createApplicationServices(androidApplicationServices, AndroidApplicationMetadata())
    }
}
