package org.archivekeep.app.desktop.ui.designsystem.layout.scrollable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
expect fun ScrollableLazyColumn(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    scrollbarPadding: PaddingValues = PaddingValues(end = 4.dp),
    content: LazyListScope.() -> Unit,
)
