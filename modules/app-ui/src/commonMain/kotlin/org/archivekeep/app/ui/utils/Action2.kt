package org.archivekeep.app.ui.utils

import androidx.compose.ui.graphics.vector.ImageVector

data class Action2(
    val icon: ImageVector,
    val title: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
)
