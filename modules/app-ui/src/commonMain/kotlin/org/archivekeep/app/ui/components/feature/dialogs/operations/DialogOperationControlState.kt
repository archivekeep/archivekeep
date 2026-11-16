package org.archivekeep.app.ui.components.feature.dialogs.operations

import kotlinx.coroutines.runBlocking
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState.Completed
import org.archivekeep.app.ui.components.feature.dialogs.operations.DialogOperationControlState.Running
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_cancelled
import org.archivekeep.ui.resources.common_failed
import org.archivekeep.ui.resources.common_finished
import org.archivekeep.utils.procedures.ProcedureExecutionState
import org.jetbrains.compose.resources.getString
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
        val outcome: String,
        val onClose: () -> Unit,
    ) : DialogOperationControlState
}

fun ProcedureExecutionState.toDialogOperationControlState(
    onCancel: (() -> Unit)?,
    onHide: (() -> Unit)?,
    onClose: () -> Unit,
): DialogOperationControlState =
    when (this) {
        ProcedureExecutionState.NotStarted, ProcedureExecutionState.Running ->
            Running(onCancel = onCancel, onHide = onHide)
        is ProcedureExecutionState.Finished ->
            Completed(outcome = outcome(), onClose = onClose)
    }

fun ProcedureExecutionState.Finished.outcome(): String =
    runBlocking {
        when (error) {
            null -> getString(Res.string.common_finished)
            is CancellationException -> getString(Res.string.common_cancelled)
            else -> getString(Res.string.common_failed)
        }
    }
