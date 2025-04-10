package org.archivekeep.app.desktop.ui.designsystem.layout.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import org.archivekeep.app.desktop.ui.designsystem.layout.scrollable.ScrollableColumn

@Composable
fun ViewScrollableContainer(content: @Composable ColumnScope.() -> Unit) {
    ScrollableColumn(
        columnPadding =
            PaddingValues(
                start = ViewPadding,
                end = ViewPadding + ViewExtraPaddingForScrollbar,
                top = ViewPadding,
                bottom = ViewPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(ViewItemSpacing),
        scrollbarPadding = ScrollbarPadding,
        content = content,
    )
}
