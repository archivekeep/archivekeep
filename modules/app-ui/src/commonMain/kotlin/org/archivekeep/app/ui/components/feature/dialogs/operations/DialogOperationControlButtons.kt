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
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_cancel
import org.archivekeep.ui.resources.common_dismiss
import org.archivekeep.ui.resources.common_hide
import org.archivekeep.ui.resources.common_launch
import org.archivekeep.ui.resources.common_running
import org.archivekeep.ui.resources.common_stop
import org.jetbrains.compose.resources.stringResource

@Composable
fun (RowScope).DialogOperationControlButtons(
    state: DialogOperationControlState,
    launchText: String = stringResource(Res.string.common_launch),
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
                    stringResource(Res.string.common_stop),
                    onClick = state.onCancel,
                )
            } else {
                DialogPrimaryButton(
                    stringResource(Res.string.common_launch),
                    onClick = {},
                    enabled = false,
                )
            }

            DialogButtonsStatusText(stringResource(Res.string.common_running), modifier = Modifier.padding(start = 8.dp))
        }

        is DialogOperationControlState.Completed -> {
            DialogButtonsStatusText(state.outcome)
        }
    }

    Spacer(modifier = Modifier.weight(1f))

    when (state) {
        is DialogOperationControlState.NotRunning -> {
            DialogDismissButton(
                stringResource(Res.string.common_cancel),
                onClick = state.onClose,
            )
        }

        is DialogOperationControlState.Running -> {
            if (state.onHide != null) {
                DialogDismissButton(
                    stringResource(Res.string.common_hide),
                    onClick = state.onHide,
                )
            }
        }

        is DialogOperationControlState.Completed -> {
            DialogDismissButton(
                stringResource(Res.string.common_dismiss),
                onClick = state.onClose,
            )
        }
    }
}
