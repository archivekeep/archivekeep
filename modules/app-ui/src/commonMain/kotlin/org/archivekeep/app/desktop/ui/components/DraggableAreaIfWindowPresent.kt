package org.archivekeep.app.desktop.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun DraggableAreaIfWindowPresent(content: @Composable () -> Unit)
