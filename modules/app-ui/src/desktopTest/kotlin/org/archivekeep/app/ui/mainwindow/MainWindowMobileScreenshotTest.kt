package org.archivekeep.app.ui.mainwindow

import androidx.compose.foundation.layout.size
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.persistence.platform.demo.phone
import org.archivekeep.app.core.persistence.platform.demo.usbStickAll
import org.archivekeep.app.core.persistence.platform.demo.usbStickDocuments
import org.archivekeep.app.core.persistence.platform.demo.usbStickMusic
import org.archivekeep.app.ui.MainWindowContent
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.utils.PropertiesApplicationMetadata
import org.archivekeep.app.ui.utils.env.runHighDensityComposeUiTestWithDemoEnv
import org.archivekeep.app.ui.utils.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.ui.utils.screenshots.setContentInMobileScreenshotContainer
import org.junit.Test

class MainWindowMobileScreenshotTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun makeScreenshot() {
        val mobileMainWindowWidth = 400.dp
        val mobileMainWindowHeight = 750.dp

        runHighDensityComposeUiTestWithDemoEnv(
            physicalMediaData = listOf(phone, usbStickAll, usbStickDocuments, usbStickMusic),
        ) { env ->
            setContentInMobileScreenshotContainer(
                Modifier.size(
                    mobileMainWindowWidth,
                    mobileMainWindowHeight,
                ),
            ) {
                ApplicationProviders(
                    applicationServices = env.services,
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
