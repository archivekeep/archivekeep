package org.archivekeep.app.desktop.ui.dialogs.sync.parts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogButtonContainer
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogDismissButton
import org.archivekeep.app.desktop.ui.designsystem.dialog.DialogPrimaryButton
import org.archivekeep.app.desktop.ui.dialogs.sync.RepoToRepoSyncUserFlow

@Composable
fun RepoToRepoSyncFlowButtons(
    userFlowState: RepoToRepoSyncUserFlow.State,
    onLaunch: () -> Unit,
    onCancel: () -> Unit,
    onClose: () -> Unit,
) {
    DialogButtonContainer {
        if (userFlowState.isCancelled) {
            Text("Cancelled ...")
            Spacer(Modifier.weight(1f))
            DialogDismissButton("Dismiss", onClick = onClose)
        } else if (userFlowState.isRunning) {
            DialogPrimaryButton(
                "Cancel",
                enabled = userFlowState.canCancel,
                onClick = onCancel,
            )
            Text("Running ...")
            Spacer(Modifier.weight(1f))
            DialogDismissButton("Hide", onClick = onClose)
        } else if (userFlowState.isCompleted) {
            Text("Completed")
            Spacer(Modifier.weight(1f))
            DialogDismissButton("Dismiss", onClose)
        } else {
            DialogPrimaryButton(
                "Launch",
                enabled = userFlowState.canLaunch,
                onClick = onLaunch,
            )
            Spacer(Modifier.weight(1f))
            DialogDismissButton("Dismiss", onClose)
        }
    }
}
