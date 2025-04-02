package org.archivekeep.app.desktop.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun DraggableAreaIfWindowPresent(content: @Composable () -> Unit) {
    content()
}
