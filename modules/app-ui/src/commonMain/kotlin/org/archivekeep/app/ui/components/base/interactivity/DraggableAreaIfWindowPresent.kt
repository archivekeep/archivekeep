package org.archivekeep.app.ui.components.base.interactivity

import androidx.compose.runtime.Composable

@Composable
expect fun DraggableAreaIfWindowPresent(content: @Composable () -> Unit)
