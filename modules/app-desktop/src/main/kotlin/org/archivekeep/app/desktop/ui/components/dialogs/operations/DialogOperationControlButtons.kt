package org.archivekeep.app.desktop.ui.components.dialogs.operations

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonsStatusText
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogContentButtonsSpacing
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogSecondaryButton
import org.archivekeep.app.desktop.ui.designsystem.styles.DesktopAppTheme

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

@Preview
@Composable
private fun preview() {
    DesktopAppTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .widthIn(max = 480.dp)
                    .background(Color.White)
                    .padding(
                        start = DialogContentButtonsSpacing + 8.dp,
                        end = DialogContentButtonsSpacing + 8.dp,
                        top = 12.dp,
                        bottom = DialogContentButtonsSpacing + 12.dp,
                    ),
            ) {
                @Composable
                fun render(controlState: DialogOperationControlState) {
                    DialogButtonContainer {
                        DialogOperationControlButtons(controlState)
                    }
                }

                @Composable
                fun previewDivider() {
                    HorizontalDivider(Modifier.padding(top = DialogContentButtonsSpacing))
                }

                render(
                    DialogOperationControlState.NotRunning(onLaunch = {}, onClose = {}, canLaunch = false),
                )
                previewDivider()
                render(
                    DialogOperationControlState.NotRunning(onLaunch = {}, onClose = {}, canLaunch = true),
                )
                previewDivider()
                render(
                    DialogOperationControlState.Running(onCancel = null, onHide = null),
                )
                previewDivider()
                render(
                    DialogOperationControlState.Running(onCancel = {}, onHide = null),
                )
                previewDivider()
                render(
                    DialogOperationControlState.Running(onCancel = null, onHide = {}),
                )
                previewDivider()
                render(
                    DialogOperationControlState.Running(onCancel = {}, onHide = {}),
                )
                previewDivider()
                render(
                    DialogOperationControlState.Completed(onClose = {}),
                )
                previewDivider()
                render(
                    DialogOperationControlState.Completed(outcome = "Cancelled", onClose = {}),
                )
            }
        }
    }
}
