package org.archivekeep.app.desktop.ui.dialogs

import androidx.compose.runtime.Composable

interface Dialog {
    @Composable
    fun render(onClose: () -> Unit)
}
