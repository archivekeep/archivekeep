package org.archivekeep.app.desktop.ui.designsystem.layout.views

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.archivekeep.app.desktop.ui.designsystem.layout.scrollable.ScrollableColumn

@Composable
fun ViewScrollableContainer(content: @Composable ColumnScope.() -> Unit) {
    ScrollableColumn(
        columnModifier = Modifier.padding(start = ViewHorizontalPadding, end = ViewHorizontalPadding + ViewExtraPaddingForScrollbar),
        scrollbarPadding = ScrollbarPadding,
        content = content,
    )
}
