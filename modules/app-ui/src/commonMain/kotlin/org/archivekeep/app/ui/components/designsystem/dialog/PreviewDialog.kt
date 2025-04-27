package org.archivekeep.app.ui.components.designsystem.dialog

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.dialogs.AbstractDialog

@Composable
fun <S : AbstractDialog.IState> AbstractDialog<S, *>.previewWith(state: S) {
    DialogPreviewColumn {
        renderDialogCard(state)
    }
}
