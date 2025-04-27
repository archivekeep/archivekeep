package org.archivekeep.app.ui.components.base.layout

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

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
    var columnSize by remember { mutableStateOf<IntSize?>(null) }

    Box(
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(scrollState)
                    .onSizeChanged { size -> columnSize = size }
                    .then(columnModifier),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content,
        )

        columnSize?.let { size ->
            VerticalScrollbar(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .height(with(LocalDensity.current) { size.height.toDp() })
                        .padding(scrollbarPadding),
                adapter = rememberScrollbarAdapter(scrollState),
            )
        }
    }
}
