package org.archivekeep.app.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.window.application
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.app.core.persistence.platform.demo.DemoApplicationServices
import org.archivekeep.app.ui.domain.services.NoOpRepositoryOpenService
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.domain.wiring.ApplicationServices
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata

private fun loadObjectBoxNativeLibrary() {
    val libDir = System.getenv("OBJECTBOX_NATIVE_LIB_DIR") ?: return
    val dir = java.io.File(libDir)
    val arch = System.getProperty("os.arch") ?: return
    val archSuffix =
        when (arch) {
            "aarch64" -> "aarch64"
            "arm64" -> "aarch64"
            "x86_64" -> "x64"
            else -> return
        }
    val lib = dir.listFiles()?.firstOrNull { it.name == "libobjectbox-jni-linux-$archSuffix.so" } ?: return
    System.load(lib.absolutePath)
}

fun main(args: Array<String>) {
    // In Flatpak, manually load the ObjectBox native library before any ObjectBox class
    // is initialized, bypassing NativeLibraryLoader which fails in the sandbox.
    loadObjectBoxNativeLibrary()
    application {
        val applicationServices: (scope: CoroutineScope, serviceWorkDispatcher: CoroutineDispatcher) -> ApplicationServices =
            remember {
                { scope, dispatcher ->
                    val isDemo = args.size == 1 && args[0] == "--demo"

                    if (isDemo) {
                        // DemoEnvironment(scope + Dispatchers.Default)
                        createGraphFactory<ApplicationServices.FromCore.Factory>().create(
                            createGraphFactory<DemoApplicationServices.Factory>().create(scope, dispatcher),
                            PropertiesApplicationMetadata(),
                            NoOpRepositoryOpenService,
                        )
                    } else {
                        createGraphFactory<DesktopApplicationServices.Factory>().create(scope, dispatcher)
                    }
                }
            }

        ApplicationProviders(applicationServices) {
            MainWindow()
        }
    }
}
