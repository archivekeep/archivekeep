package org.archivekeep.app.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.window.application
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.platform.demo.DemoApplicationServices
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.domain.wiring.createApplicationServices
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata

fun main(args: Array<String>) {
    application {
        val applicationServices: (scope: CoroutineScope, serviceWorkDispatcher: CoroutineDispatcher) -> ApplicationServices =
            remember {
                { scope, dispatcher ->
                    val isDemo = args.size == 1 && args[0] == "--demo"

                    if (isDemo) {
                        // DemoEnvironment(scope + Dispatchers.Default)
                        createApplicationServices(
                            createGraphFactory<DemoApplicationServices.Factory>().create(scope, dispatcher),
                        )
                    } else {
                        createGraphFactory<DesktopApplicationServices.Factory>().create(scope, dispatcher)
                    }
                }
            }

        val applicationMetadata = remember { PropertiesApplicationMetadata() }

        ApplicationProviders(applicationServices, applicationMetadata) {
            MainWindow()
        }
    }
}
