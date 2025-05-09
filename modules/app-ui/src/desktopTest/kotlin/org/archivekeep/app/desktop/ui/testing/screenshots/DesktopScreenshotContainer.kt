package org.archivekeep.app.desktop.ui.testing.screenshots

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme

@Composable
fun DesktopScreenshotContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AppTheme {
        ScreenshotContainer(modifier, Modifier.padding(20.dp), content)
    }
}
