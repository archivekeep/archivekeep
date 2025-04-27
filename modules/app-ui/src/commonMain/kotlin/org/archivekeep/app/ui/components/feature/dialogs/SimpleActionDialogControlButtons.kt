package org.archivekeep.app.ui.components.feature.dialogs

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.ui.components.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.ui.utils.ActionTriggerState

@Composable
fun RowScope.SimpleActionDialogControlButtons(
    launchText: String,
    actionState: ActionTriggerState,
    onClose: () -> Unit,
) {
    SimpleActionDialogControlButtons(
        launchText,
        onLaunch = actionState.onLaunch,
        onClose = onClose,
        canLaunch = actionState.canLaunch,
        isRunning = actionState.isRunning,
    )
}

@Composable
fun RowScope.SimpleActionDialogControlButtons(
    launchText: String,
    onLaunch: () -> Unit,
    onClose: () -> Unit,
    canLaunch: Boolean = true,
    isRunning: Boolean = false,
) {
    DialogPrimaryButton(
        launchText,
        onClick = onLaunch,
        enabled = canLaunch && !isRunning,
    )

    if (isRunning) {
        CircularProgressIndicator(
            Modifier
                .padding(start = 6.dp)
                .size(24.dp)
                .align(Alignment.CenterVertically),
        )
    }

    Spacer(modifier = Modifier.weight(1f))

    DialogDismissButton(
        "Cancel",
        onClick = onClose,
    )
}
