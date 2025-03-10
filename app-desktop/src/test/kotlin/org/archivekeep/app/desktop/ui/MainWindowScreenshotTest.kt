package org.archivekeep.app.desktop.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import org.archivekeep.app.core.persistence.drivers.filesystem.FileStores
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.desktop.domain.wiring.ApplicationProviders
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.desktop.ui.testing.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.desktop.ui.testing.screenshots.setContentScreenshotContainer
import org.junit.Test

class MainWindowScreenshotTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun makeScreenshot() {
        runHighDensityComposeUiTest {
            setContentScreenshotContainer(
                Modifier.size(DefaultMainWindowWidth, DefaultMainWindowHeight),
            ) {
                val scope = rememberCoroutineScope()
                val fileStores = remember { FileStores(scope) }

                ApplicationProviders(
                    scope,
                    DemoEnvironment(
                        scope + Dispatchers.Default,
                        enableSpeedLimit = false,
                    ),
                    fileStores,
                ) {
                    MainWindowContent(
                        isFloating = true,
                        onCloseRequest = {},
                    )
                }
            }

            saveTestingContainerBitmap("main-window.png")
        }
    }
}
