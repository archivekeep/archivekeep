package org.archivekeep.app.ui.components.designsystem.dialog

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable

@Composable
fun DialogOverlayCard(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DialogOverlay(
        onDismissRequest = onDismissRequest,
    ) {
        DialogCard(content = content)
    }
}
