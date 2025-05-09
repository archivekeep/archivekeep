package org.archivekeep.app.desktop.ui.testing.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.base.layout.LocalEnableScrollbar
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme
import org.archivekeep.app.ui.components.designsystem.theme.LocalAppDimens

@Composable
fun MobileScreenshotContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AppTheme {
        CompositionLocalProvider(
            LocalEnableScrollbar provides false,
            LocalAppDimens provides LocalAppDimens.current.copy(viewExtraPaddingForScrollbar = 0.dp),
        ) {
            ScreenshotContainer(
                modifier = modifier,
                innerModifier = Modifier,
                content = content,
            )
        }
    }
}
