package org.archivekeep.app.ui.components.feature.dialogs.operations

import org.archivekeep.app.core.utils.operations.OperationExecutionState
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState.Completed
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState.Running
import java.util.concurrent.CancellationException

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

fun OperationExecutionState.toDialogOperationControlState(
    onCancel: (() -> Unit)?,
    onHide: (() -> Unit)?,
    onClose: () -> Unit,
): DialogOperationControlState =
    when (this) {
        OperationExecutionState.NotStarted, OperationExecutionState.Running ->
            Running(onCancel = onCancel, onHide = onHide)
        is OperationExecutionState.Finished ->
            Completed(outcome = outcome(), onClose = onClose)
    }

fun OperationExecutionState.Finished.outcome(): String =
    when (error) {
        null -> "Finished"
        is CancellationException -> "Cancelled"
        else -> "Failed"
    }
