package org.archivekeep.app.desktop.ui.dialogs.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import org.archivekeep.app.desktop.ui.testing.screenshots.ScreenshotContainer
import org.archivekeep.app.desktop.ui.testing.screenshots.runHighDensityComposeUiTest
import org.archivekeep.app.desktop.ui.testing.screenshots.saveTestingContainerBitmap

@Composable
fun DialogScreenshotContainer(content: @Composable () -> Unit) {
    ScreenshotContainer(content = content)
}

@OptIn(ExperimentalTestApi::class)
fun (ComposeUiTest).setContentInDialogScreenshotContainer(content: @Composable () -> Unit) {
    this.setContent {
        DialogScreenshotContainer(content = content)
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
