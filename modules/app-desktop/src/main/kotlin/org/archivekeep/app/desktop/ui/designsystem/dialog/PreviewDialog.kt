package org.archivekeep.app.desktop.ui.designsystem.dialog

import androidx.compose.runtime.Composable
import org.archivekeep.app.desktop.ui.dialogs.AbstractDialog

@Composable
fun <S : AbstractDialog.IState> AbstractDialog<S, *>.previewWith(state: S) {
    DialogPreviewColumn {
        renderDialogCard(state)
    }
}
