package org.archivekeep.app.ui.components.feature.dialogs.operations

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.dialog.DialogButtonsStatusText
import org.archivekeep.app.ui.components.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.ui.components.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.ui.components.designsystem.dialog.DialogSecondaryButton

@Composable
fun (RowScope).DialogOperationControlButtons(
    state: DialogOperationControlState,
    launchText: String = "Launch",
) {
    when (state) {
        is DialogOperationControlState.NotRunning -> {
            DialogPrimaryButton(
                launchText,
                onClick = state.onLaunch,
                enabled = state.canLaunch,
            )
        }

        is DialogOperationControlState.Running -> {
            if (state.onCancel != null) {
                DialogSecondaryButton(
                    "Stop",
                    onClick = state.onCancel,
                )
            } else {
                DialogPrimaryButton(
                    "Launch",
                    onClick = {},
                    enabled = false,
                )
            }

            DialogButtonsStatusText("Running …", modifier = Modifier.padding(start = 8.dp))
        }

        is DialogOperationControlState.Completed -> {
            DialogButtonsStatusText(state.outcome)
        }
    }

    Spacer(modifier = Modifier.weight(1f))

    when (state) {
        is DialogOperationControlState.NotRunning -> {
            DialogDismissButton(
                "Cancel",
                onClick = state.onClose,
            )
        }

        is DialogOperationControlState.Running -> {
            if (state.onHide != null) {
                DialogDismissButton(
                    "Hide",
                    onClick = state.onHide,
                )
            }
        }

        is DialogOperationControlState.Completed -> {
            DialogDismissButton(
                "Dismiss",
                onClick = state.onClose,
            )
        }
    }
}
