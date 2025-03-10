package org.archivekeep.app.desktop.ui.components

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.WindowScope
import org.archivekeep.app.desktop.domain.wiring.LocalOptionalComposeWindow
import java.awt.Window

@Composable
fun DraggableAreaIfWindowPresent(content: @Composable () -> Unit) {
    val window = LocalOptionalComposeWindow.current

    if (window != null) {
        val scope =
            object : WindowScope {
                override val window: Window = window
            }

        scope.WindowDraggableArea(content = content)
    } else {
        content()
    }
}
