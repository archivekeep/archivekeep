package org.archivekeep.app.desktop.ui.components.dialogs

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton

@Composable
fun RowScope.SimpleActionDialogControlButtons(
    launchText: String,
    onLaunch: () -> Unit,
    onClose: () -> Unit,
    canLaunch: Boolean = true,
) {
    DialogPrimaryButton(
        launchText,
        onClick = onLaunch,
        enabled = canLaunch,
    )

    Spacer(modifier = Modifier.weight(1f))

    DialogDismissButton(
        "Cancel",
        onClick = onClose,
    )
}
