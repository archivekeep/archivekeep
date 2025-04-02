package org.archivekeep.app.desktop.ui

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.desktop.DefaultMainWindowHeight
import org.archivekeep.app.desktop.DefaultMainWindowWidth
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
                ApplicationProviders(
                    environmentFactory = { scope ->
                        DemoEnvironment(
                            scope,
                            enableSpeedLimit = false,
                        )
                    },
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
