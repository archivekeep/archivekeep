package org.archivekeep.app.desktop.ui.designsystem.dialog

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString

@Composable
fun DialogCardWithDialogInnerContainer(
    title: AnnotatedString,
    content: @Composable ColumnScope.() -> Unit,
    bottomContent: @Composable () -> Unit,
) {
    DialogCard {
        DialogInnerContainer(title, content, bottomContent)
    }
}
