package org.archivekeep.app.ui.components.base.interactivity

import androidx.compose.runtime.Composable

@Composable
actual fun DraggableAreaIfWindowPresent(content: @Composable () -> Unit) {
    content()
}
