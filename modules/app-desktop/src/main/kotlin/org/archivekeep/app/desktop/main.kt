package org.archivekeep.app.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.archivekeep.app.core.persistence.platform.Environment
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders

fun main(args: Array<String>) {
    application {
        val environment: (scope: CoroutineScope) -> Environment =
            remember {
                { scope ->
                    val isDemo = args.size == 1 && args[0] == "--demo"

                    if (isDemo) {
                        DemoEnvironment(scope + Dispatchers.Default)
                    } else {
                        DesktopEnvironment(
                            scope + Dispatchers.Default,
                        )
                    }
                }
            }

        ApplicationProviders(environment) {
            MainWindow()
        }
    }
}
