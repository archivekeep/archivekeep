package org.archivekeep.app.ui.dialogs

import androidx.compose.runtime.Composable

interface Dialog {
    @Composable
    fun render(onClose: () -> Unit)
}
