package org.archivekeep.app.ui.utils

data class ActionTriggerState(
    val onLaunch: () -> Unit,
    val canLaunch: Boolean = true,
    val isRunning: Boolean,
)
