package org.archivekeep.app.desktop.utils

data class Action(
    val onLaunch: () -> Unit,
    val text: String,
    val isAvailable: Boolean = true,
    val running: Boolean = false,
)
