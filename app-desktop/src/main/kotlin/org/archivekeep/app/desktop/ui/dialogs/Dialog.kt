package org.archivekeep.app.desktop.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow

interface Dialog {
    @Composable
    fun render(
        window: ComposeWindow,
        onClose: () -> Unit,
    )
}
