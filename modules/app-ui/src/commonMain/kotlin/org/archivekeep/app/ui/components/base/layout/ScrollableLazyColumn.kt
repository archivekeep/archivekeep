package org.archivekeep.app.ui.components.base.layout

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ScrollableLazyColumn(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    horizontalScrollState: ScrollState? = null,
    scrollbarPadding: PaddingValues = scrollableLazyColumnPadding,
    content: LazyListScope.() -> Unit,
)
