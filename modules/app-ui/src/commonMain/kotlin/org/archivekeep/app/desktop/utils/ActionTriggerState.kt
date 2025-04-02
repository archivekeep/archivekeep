package org.archivekeep.app.desktop.utils

data class ActionTriggerState(
    val onLaunch: () -> Unit,
    val canLaunch: Boolean = true,
    val isRunning: Boolean,
)
