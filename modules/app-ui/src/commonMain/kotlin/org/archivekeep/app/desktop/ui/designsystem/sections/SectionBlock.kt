package org.archivekeep.app.desktop.ui.designsystem.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

@Composable
fun SectionBlock(
    text: String,
    buttons: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        SectionTitle(text, buttons)

        content()
    }
}
