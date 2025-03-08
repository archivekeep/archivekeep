package org.archivekeep.app.desktop.ui.dialogs.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.desktop.ui.designsystem.styles.DesktopAppTheme
import org.archivekeep.app.desktop.ui.testing.save
import org.jetbrains.skiko.toBufferedImage
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories

private val semanticsKey = SemanticsPropertyKey<GraphicsLayer>("DialogScreenshotContainerGraphicsLayer")

@Composable
fun DialogScreenshotContainer(content: @Composable () -> Unit) {
    val graphicsLayer = rememberGraphicsLayer()

    DesktopAppTheme {
        Box(
            Modifier
                .width(IntrinsicSize.Max)
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Blue)
                        .semantics {
                            set(semanticsKey, graphicsLayer)
                        }.drawWithContent {
                            graphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(graphicsLayer)
                        }.padding(20.dp),
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun runHighDensityComposeUiTest(block: SkikoComposeUiTest.() -> Unit) {
    runSkikoComposeUiTest(
        size = Size(2048.0f, 1536.0f),
        density = Density(2f),
        block = block,
    )
}

@OptIn(ExperimentalTestApi::class)
fun (ComposeUiTest).setContentInDialogScreenshotContainer(content: @Composable () -> Unit) {
    this.setContent {
        DialogScreenshotContainer(content = content)
    }
}

@OptIn(ExperimentalTestApi::class)
fun (ComposeUiTest).saveDialogBitmap(filename: String) {
    val path = Path(System.getenv("SCREENSHOTS_BUILD_OUTPUT")).resolve(filename)

    path.createParentDirectories()

    this.runOnIdle {
        val graphicsLayer =
            this
                .onNode(SemanticsMatcher.keyIsDefined(semanticsKey))
                .fetchSemanticsNode()
                .config
                .getOrNull(semanticsKey)
                ?: throw RuntimeException("Not found container created with DialogScreenshotContainer or setContentInDialogScreenshotContainer")

        runOnUiThread { waitForIdle() }

        runBlocking {
            graphicsLayer
                .toImageBitmap()
                .asSkiaBitmap()
                .toBufferedImage()
                .save(path)
        }
    }
}
