package org.archivekeep.app.ui.components.feature.dialogs

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.dialog.DialogDismissButton

@Composable
fun RowScope.SimpleActionDialogDoneButtons(onClose: () -> Unit) {
    DialogDismissButton(
        "Close",
        onClick = onClose,
    )
}
