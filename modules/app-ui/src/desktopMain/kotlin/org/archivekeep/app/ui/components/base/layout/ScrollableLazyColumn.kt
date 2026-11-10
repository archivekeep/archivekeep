package org.archivekeep.app.ui.components.base.layout

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun ScrollableLazyColumn(
    modifier: Modifier,
    columnModifier: Modifier,
    state: LazyListState,
    horizontalScrollState: ScrollState?,
    scrollbarPadding: PaddingValues,
    content: LazyListScope.() -> Unit,
) {
    ScrollableContainer(
        modifier = modifier,
        scrollbarPadding = scrollbarPadding,
        scrollbarAdapter = rememberScrollbarAdapter(state),
        horizontalScrollState = horizontalScrollState,
    ) { scrollModifier ->
        LazyColumn(
            modifier =
                columnModifier
                    .fillMaxWidth()
                    .then(scrollModifier)
                    .then(
                        if (horizontalScrollState != null) {
                            Modifier.horizontalScroll(horizontalScrollState)
                        } else {
                            Modifier
                        },
                    ),
            state = state,
            content = content,
        )
    }
}
