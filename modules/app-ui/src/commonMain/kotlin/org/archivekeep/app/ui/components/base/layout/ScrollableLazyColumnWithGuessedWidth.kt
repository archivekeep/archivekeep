package org.archivekeep.app.ui.components.base.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun ScrollableLazyColumnWithGuessedWidth(
    guessedWidth: Dp,
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    enableHorizontalScroll: Boolean = false,
    scrollbarPadding: PaddingValues = scrollableLazyColumnPadding,
    content: LazyListScope.() -> Unit,
) {
    val horizontalScrollState = rememberScrollState()

    IntrinsicSizeWrapperLayout(
        minIntrinsicWidth = guessedWidth,
        maxIntrinsicWidth = guessedWidth,
    ) {
        ScrollableLazyColumn(
            modifier = modifier,
            columnModifier = columnModifier,
            state = state,
            horizontalScrollState = if(enableHorizontalScroll) horizontalScrollState else null,
            scrollbarPadding = scrollbarPadding,
            content = content,
        )
    }
}
