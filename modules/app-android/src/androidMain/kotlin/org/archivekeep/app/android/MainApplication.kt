package org.archivekeep.app.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import org.archivekeep.app.desktop.domain.wiring.ApplicationServices

class MainApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob())

    lateinit var services: ApplicationServices

    override fun onCreate() {
        super.onCreate()

        services =
            ApplicationServices(scope) { scope ->
                AndroidEnvironment(
                    scope + Dispatchers.Default,
                    AndroidEnvironmentPaths(filesDir),
                )
            }
    }
}
