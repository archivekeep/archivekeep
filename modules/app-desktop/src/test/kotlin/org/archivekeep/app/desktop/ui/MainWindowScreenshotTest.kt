package org.archivekeep.app.desktop.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.DpSize
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.desktop.DefaultMainWindowHeight
import org.archivekeep.app.desktop.DefaultMainWindowWidth
import org.archivekeep.app.desktop.DesktopApplicationMetadata
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.desktop.ui.testing.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.desktop.ui.testing.screenshots.setContentScreenshotContainer
import org.archivekeep.app.ui.MainWindowContent
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.junit.Test

class MainWindowScreenshotTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
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
                    applicationMetadata = DesktopApplicationMetadata(),
                ) {
                    MainWindowContent(
                        isFloating = true,
                        windowSizeClass =
                            WindowSizeClass.calculateFromSize(
                                DpSize(DefaultMainWindowWidth, DefaultMainWindowHeight),
                            ),
                        onCloseRequest = {},
                    )
                }
            }

            saveTestingContainerBitmap("main-window.png")
        }
    }
}
