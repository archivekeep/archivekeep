package org.archivekeep.app.desktop.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.DpSize
import dev.zacsweers.metro.createGraphFactory
import org.archivekeep.app.core.persistence.platform.demo.DemoApplicationServices
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.desktop.ui.testing.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.desktop.ui.testing.screenshots.setContentScreenshotContainer
import org.archivekeep.app.ui.MainWindowContent
import org.archivekeep.app.ui.domain.wiring.ApplicationProvidersFromCore
import org.archivekeep.app.ui.utils.DefaultMainWindowHeight
import org.archivekeep.app.ui.utils.DefaultMainWindowWidth
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.junit.Test

class MainWindowDesktopScreenshotTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun makeScreenshot() {
        runHighDensityComposeUiTest {
            setContentScreenshotContainer(
                Modifier.size(
                    DefaultMainWindowWidth,
                    DefaultMainWindowHeight,
                ),
            ) {
                ApplicationProvidersFromCore(
                    coreApplicationServicesFactory = { scope, dispatcher ->
                        createGraphFactory<DemoApplicationServices.Factory>().create(
                            scope,
                            dispatcher,
                            enableSpeedLimit = false,
                        )
                    },
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    MainWindowContent(
                        isFloating = true,
                        windowSizeClass =
                            WindowSizeClass.calculateFromSize(
                                DpSize(
                                    DefaultMainWindowWidth,
                                    DefaultMainWindowHeight,
                                ),
                            ),
                        onCloseRequest = {},
                    )
                }
            }

            saveTestingContainerBitmap("desktop/main-window.png")
        }
    }
}
