package org.archivekeep.app.desktop.ui.designsystem.layout.scrollable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun ScrollableLazyColumn(
    modifier: Modifier,
    columnModifier: Modifier,
    state: LazyListState,
    scrollbarPadding: PaddingValues,
    content: LazyListScope.() -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = columnModifier.fillMaxWidth(),
            state = state,
            content = content,
        )
    }
}
