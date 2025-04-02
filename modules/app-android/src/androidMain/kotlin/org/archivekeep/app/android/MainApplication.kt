package org.archivekeep.app.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.desktop.domain.wiring.ApplicationServices

class MainApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob())

    val services = ApplicationServices(scope) { scope -> DemoEnvironment(scope + Dispatchers.Default) }
}
