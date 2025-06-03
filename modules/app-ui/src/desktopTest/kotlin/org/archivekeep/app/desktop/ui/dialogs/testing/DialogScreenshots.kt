package org.archivekeep.app.desktop.ui.dialogs.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import org.archivekeep.app.desktop.ui.testing.screenshots.DesktopScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.desktop.ui.testing.screenshots.saveTestingContainerBitmap
import org.archivekeep.app.ui.components.designsystem.dialog.LocalDialogOverlayContentsWrapper

val dialogOverlayContentsSemanticsKey = SemanticsPropertyKey<GraphicsLayer>("DialogContainerGraphicsLayer")

@Composable
fun DialogScreenshotContainer(content: @Composable () -> Unit) {
    DesktopScreenshotContainer(content = content)
}

@OptIn(ExperimentalTestApi::class)
fun (ComposeUiTest).setContentInDialogScreenshotContainer(content: @Composable () -> Unit) {
    this.setContent {
        CompositionLocalProvider(
            LocalDialogOverlayContentsWrapper provides { content ->
                val graphicsLayer = rememberGraphicsLayer()

                Box(
                    modifier =
                        Modifier
                            .semantics {
                                set(dialogOverlayContentsSemanticsKey, graphicsLayer)
                            }.drawWithContent {
                                graphicsLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                                drawLayer(graphicsLayer)
                            },
                ) {
                    content()
                }
            },
        ) {
            DialogScreenshotContainer(content = content)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun makeDialogScreenshot(
    filename: String,
    content: @Composable () -> Unit,
) {
    runHighDensityComposeUiTest {
        setContentInDialogScreenshotContainer(content = content)
        saveTestingContainerBitmap(filename)
    }
}

@OptIn(ExperimentalTestApi::class)
fun (ComposeUiTest).saveTestingDialogContainerBitmap(filename: String) {
    saveTestingContainerBitmap(filename, dialogOverlayContentsSemanticsKey)
}
