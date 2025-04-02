package org.archivekeep.app.desktop.ui.designsystem.layout.scrollable

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun ScrollableColumn(
    modifier: Modifier,
    columnModifier: Modifier,
    scrollState: ScrollState,
    scrollbarPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = columnModifier.verticalScroll(scrollState),
            content = content,
        )
    }
}
