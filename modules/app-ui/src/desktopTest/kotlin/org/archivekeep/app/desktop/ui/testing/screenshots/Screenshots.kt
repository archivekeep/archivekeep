package org.archivekeep.app.desktop.ui.testing.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.runBlocking
import org.archivekeep.app.desktop.ui.testing.save
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories

private val semanticsKey = SemanticsPropertyKey<GraphicsLayer>("ScreenshotContainerGraphicsLayer")

@Composable
fun ScreenshotContainer(
    modifier: Modifier = Modifier,
    innerModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val graphicsLayer = rememberGraphicsLayer()

    Box {
        Box(
            modifier =
                modifier
                    .semantics {
                        set(semanticsKey, graphicsLayer)
                    }.drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }.then(innerModifier),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun runHighDensityComposeUiTest(
    sizeNotScaled: Size = Size(1200.0f, 800.0f),
    block: SkikoComposeUiTest.() -> Unit,
) {
    runSkikoComposeUiTest(
        size = Size(sizeNotScaled.width * 2, sizeNotScaled.height * 2),
        density = Density(2f),
        block = block,
    )
}

@OptIn(ExperimentalTestApi::class)
fun (ComposeUiTest).setContentScreenshotContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    this.setContent {
        DesktopScreenshotContainer(modifier = modifier, content = content)
    }
}

@OptIn(ExperimentalTestApi::class)
fun (ComposeUiTest).setContentInMobileScreenshotContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    this.setContent {
        MobileScreenshotContainer(modifier = modifier, content = content)
    }
}

@OptIn(ExperimentalTestApi::class)
fun (ComposeUiTest).saveTestingContainerBitmap(
    filename: String,
    key: SemanticsPropertyKey<GraphicsLayer> = semanticsKey,
) {
    val path = Path(System.getenv("SCREENSHOTS_BUILD_OUTPUT")).resolve(filename)

    path.createParentDirectories()

    this.runOnIdle {
        val graphicsLayer =
            this
                .onNode(SemanticsMatcher.keyIsDefined(key))
                .fetchSemanticsNode()
                .config
                .getOrNull(key)
                ?: throw RuntimeException("Not found container created with ScreenshotContainer or saveTestingContainerBitmap")

        runBlocking {
            graphicsLayer
                .toImageBitmap()
                .toAwtImage()
                .save(path)
        }
    }
}
