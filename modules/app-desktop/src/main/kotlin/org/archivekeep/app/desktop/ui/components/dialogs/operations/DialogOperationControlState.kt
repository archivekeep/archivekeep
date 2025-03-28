package org.archivekeep.app.desktop.ui.components.dialogs.operations

sealed interface DialogOperationControlState {
    data class NotRunning(
        val onLaunch: () -> Unit,
        val onClose: () -> Unit,
        val canLaunch: Boolean = true,
    ) : DialogOperationControlState

    data class Running(
        val onCancel: (() -> Unit)? = null,
        val onHide: (() -> Unit)? = null,
    ) : DialogOperationControlState

    data class Completed(
        val outcome: String = "Completed",
        val onClose: () -> Unit,
    ) : DialogOperationControlState
}
