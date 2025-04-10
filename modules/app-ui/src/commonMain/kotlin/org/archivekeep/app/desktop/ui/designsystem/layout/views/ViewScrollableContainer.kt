package org.archivekeep.app.desktop.ui.designsystem.layout.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.archivekeep.app.desktop.ui.designsystem.layout.scrollable.ScrollableColumn

@Composable
fun ViewScrollableContainer(content: @Composable ColumnScope.() -> Unit) {
    ScrollableColumn(
        columnModifier =
            Modifier
                .consumeWindowInsets(
                    PaddingValues(
                        top = ViewPadding / 2,
                        start = ViewPadding,
                        end = ViewPadding + ViewExtraPaddingForScrollbar,
                        bottom = ViewPadding,
                    ),
                ).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .windowInsetsPadding(WindowInsets.safeGestures.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(
                    PaddingValues(
                        start = ViewPadding,
                        end = ViewPadding + ViewExtraPaddingForScrollbar,
                        top = ViewPadding,
                        bottom = ViewPadding,
                    ),
                ),
        verticalArrangement = Arrangement.spacedBy(ViewItemSpacing),
        scrollbarPadding = ScrollbarPadding,
        content = content,
    )
}
