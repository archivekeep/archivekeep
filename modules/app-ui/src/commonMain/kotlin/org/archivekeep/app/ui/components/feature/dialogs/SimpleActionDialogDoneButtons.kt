package org.archivekeep.app.ui.components.feature.dialogs

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.components.designsystem.dialog.DialogDismissButton
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_close
import org.jetbrains.compose.resources.stringResource

@Composable
fun RowScope.SimpleActionDialogDoneButtons(onClose: () -> Unit) {
    DialogDismissButton(
        stringResource(Res.string.common_close),
        onClick = onClose,
    )
}
