package org.archivekeep.app.ui.components.base.layout

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
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
actual fun ScrollableLazyColumn(
    modifier: Modifier,
    columnModifier: Modifier,
    state: LazyListState,
    scrollbarPadding: PaddingValues,
    content: LazyListScope.() -> Unit,
) {
    var columnSize by remember { mutableStateOf<IntSize?>(null) }
    Box(
        modifier = modifier,
    ) {
        LazyColumn(
            modifier =
                columnModifier
                    .fillMaxWidth()
                    .onSizeChanged { size -> columnSize = size },
            state = state,
            content = content,
        )

        columnSize?.let { size ->
            VerticalScrollbar(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .height(with(LocalDensity.current) { size.height.toDp() })
                        .padding(scrollbarPadding),
                adapter = rememberScrollbarAdapter(state),
            )
        }
    }
}
