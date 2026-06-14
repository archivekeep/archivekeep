package org.archivekeep.app.ui.utils.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.dialog.LocalDialogOverlayContentsWrapper
import org.archivekeep.app.ui.components.designsystem.theme.AppTheme

@OptIn(ExperimentalTestApi::class)
fun (ComposeUiTest).setContentInDialogScreenshotContainer(content: @Composable () -> Unit) {
    this.setContent {
        CompositionLocalProvider(
            LocalDialogOverlayContentsWrapper provides { content ->
                Box(modifier = Modifier.screenshootContainer()) {
                    content()
                }
            },
        ) {
            AppTheme {
                Box(Modifier.padding(20.dp)) {
                    content()
                }
            }
        }
    }
}
