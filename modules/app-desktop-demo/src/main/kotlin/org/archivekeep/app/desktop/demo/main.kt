package org.archivekeep.app.desktop.demo

import androidx.compose.runtime.remember
import androidx.compose.ui.window.application
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.platform.demo.DemoApplicationServices
import org.archivekeep.app.ui.desktop.MainWindow
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.domain.wiring.createApplicationServices
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata

private fun createDemoApplicationServices(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
): ApplicationServices =
    createApplicationServices(
        createGraphFactory<DemoApplicationServices.Factory>().create(scope, dispatcher),
    )

fun main() {
    application {
        val applicationMetadata = remember { PropertiesApplicationMetadata() }

        ApplicationProviders(::createDemoApplicationServices, applicationMetadata) {
            MainWindow()
        }
    }
}
