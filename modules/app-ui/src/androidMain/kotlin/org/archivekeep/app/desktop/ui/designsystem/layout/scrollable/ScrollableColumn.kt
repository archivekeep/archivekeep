package org.archivekeep.app.desktop.ui.designsystem.layout.scrollable

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun ScrollableColumn(
    modifier: Modifier,
    columnModifier: Modifier,
    verticalArrangement: Arrangement.Vertical,
    horizontalAlignment: Alignment.Horizontal,
    scrollState: ScrollState,
    scrollbarPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState).then(columnModifier),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}
