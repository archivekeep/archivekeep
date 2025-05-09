package org.archivekeep.app.desktop.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.persistence.platform.demo.DemoEnvironment
import org.archivekeep.app.core.persistence.platform.demo.flashAll
import org.archivekeep.app.core.persistence.platform.demo.flashDocuments
import org.archivekeep.app.core.persistence.platform.demo.flashMusic
import org.archivekeep.app.core.persistence.platform.demo.phone
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.desktop.ui.testing.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.desktop.ui.testing.screenshots.setContentInMobileScreenshotContainer
import org.archivekeep.app.ui.MainWindowContent
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.junit.Test

class MainWindowMobileScreenshotTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun makeScreenshot() {
        val mobileMainWindowWidth = 400.dp
        val mobileMainWindowHeight = 750.dp

        runHighDensityComposeUiTest {
            setContentInMobileScreenshotContainer(
                Modifier.size(
                    mobileMainWindowWidth,
                    mobileMainWindowHeight,
                ),
            ) {
                ApplicationProviders(
                    environmentFactory = { scope ->
                        DemoEnvironment(
                            scope,
                            physicalMediaData = listOf(phone, flashAll, flashDocuments, flashMusic),
                            enableSpeedLimit = false,
                        )
                    },
                    applicationMetadata = PropertiesApplicationMetadata(),
                ) {
                    MainWindowContent(
                        isFloating = false,
                        windowSizeClass =
                            WindowSizeClass.calculateFromSize(
                                DpSize(
                                    mobileMainWindowWidth,
                                    mobileMainWindowHeight,
                                ),
                            ),
                        onCloseRequest = null,
                    )
                }
            }

            saveTestingContainerBitmap("mobile/main-window.png")
        }
    }
}
