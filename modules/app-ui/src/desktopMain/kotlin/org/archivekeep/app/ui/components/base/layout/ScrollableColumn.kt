package org.archivekeep.app.ui.components.base.layout

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollbarAdapter
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
    ScrollableContainer(
        modifier = modifier,
        scrollbarPadding = scrollbarPadding,
        scrollbarAdapter = rememberScrollbarAdapter(scrollState),
    ) { scrollModifier ->
        Column(
            modifier =
                Modifier
                    .verticalScroll(scrollState)
                    .then(scrollModifier)
                    .then(columnModifier),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )
    }
}
