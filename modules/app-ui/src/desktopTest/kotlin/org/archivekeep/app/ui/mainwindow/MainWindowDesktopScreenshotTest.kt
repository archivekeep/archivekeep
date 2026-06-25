package org.archivekeep.app.ui.mainwindow

import androidx.compose.foundation.layout.size
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.waitUntilNodeCount
import androidx.compose.ui.unit.DpSize
import org.archivekeep.app.ui.MainWindowContent
import org.archivekeep.app.ui.domain.wiring.ApplicationProviders
import org.archivekeep.app.ui.utils.DefaultMainWindowHeight
import org.archivekeep.app.ui.utils.DefaultMainWindowWidth
import org.archivekeep.app.ui.utils.env.runHighDensityComposeUiTestWithDemoEnv
import org.archivekeep.app.ui.utils.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.ui.utils.screenshots.setContentScreenshotContainer
import org.junit.Test

class MainWindowDesktopScreenshotTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
    @Test
    fun makeScreenshot() {
        runHighDensityComposeUiTestWithDemoEnv { env ->
            setContentScreenshotContainer(
                Modifier.size(
                    DefaultMainWindowWidth,
                    DefaultMainWindowHeight,
                ),
            ) {
                ApplicationProviders(env.services) {
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

            waitUntilNodeCount(
                hasText("loading", substring = true, ignoreCase = true) or SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
                0,
            )

            saveTestingContainerBitmap("desktop/main-window.png")
        }
    }
}
