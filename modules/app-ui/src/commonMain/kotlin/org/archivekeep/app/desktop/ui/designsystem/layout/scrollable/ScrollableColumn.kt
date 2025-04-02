package org.archivekeep.app.desktop.ui.designsystem.layout.scrollable

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
expect fun ScrollableColumn(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    scrollbarPadding: PaddingValues = PaddingValues(end = 4.dp),
    content: @Composable ColumnScope.() -> Unit,
)
