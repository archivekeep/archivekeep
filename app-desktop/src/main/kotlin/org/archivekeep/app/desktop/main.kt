package org.archivekeep.app.desktop

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.application
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.platform.desktop.DesktopEnvironment
import org.archivekeep.app.desktop.domain.wiring.ApplicationProviders
import org.archivekeep.app.desktop.ui.MainWindow

fun main(args: Array<String>) {
    application {
        val scope = rememberCoroutineScope()

        val fileStores = remember { FileStores(scope) }

        val environment =
            remember {
                val isDemo = args.size == 1 && args[0] == "--demo"

                if (isDemo) {
                    DemoEnvironment(scope)
                } else {
                    DesktopEnvironment(
                        scope,
                        fileStores,
                    )
                }
            }

        ApplicationProviders(
            scope,
            environment,
            fileStores,
        ) {
            MainWindow()
        }
    }
}
