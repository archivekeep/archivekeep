package org.archivekeep.app.ui.utils

import androidx.compose.ui.graphics.vector.ImageVector

data class Action(
    val icon: ImageVector? = null,
    val title: String,
    val onLaunch: () -> Unit,
    val isPending: Boolean = true,
    val isAvailable: Boolean = true,
    val running: Boolean = false,
)
