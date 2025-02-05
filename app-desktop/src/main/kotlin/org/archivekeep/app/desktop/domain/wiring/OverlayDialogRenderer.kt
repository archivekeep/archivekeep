package org.archivekeep.app.desktop.domain.wiring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import org.archivekeep.app.desktop.ui.dialogs.Dialog

class OverlayDialogRenderer {
    private var openedDialogs by mutableStateOf(listOf<Dialog>())

    fun openDialog(dialog: Dialog) {
        openedDialogs = openedDialogs + listOf(dialog)
    }

    @Composable
    fun render(window: ComposeWindow) {
        openedDialogs.forEach { dialog ->
            dialog.render(
                window,
                onClose = { openedDialogs = openedDialogs.filter { it != dialog } },
            )
        }
    }

    fun openFn(c: () -> Dialog): () -> Unit =
        {
            openDialog(c())
        }

    fun <A> openFn(c: (A) -> Dialog): (A) -> Unit =
        { a ->
            openDialog(c(a))
        }

    fun <A, B> openFn(c: (A, B) -> Dialog): (A, B) -> Unit =
        { a, b ->
            openDialog(c(a, b))
        }
}
