package org.archivekeep.app.ui.components.designsystem.dialog

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString

@Composable
fun DialogCardWithDialogInnerContainer(
    title: AnnotatedString,
    widthModifier: Modifier = defaultDialogWidthModifier,
    content: @Composable ColumnScope.() -> Unit,
    bottomContent: @Composable () -> Unit,
) {
    DialogCard {
        DialogInnerContainer(title, widthModifier, content, bottomContent)
    }
}
