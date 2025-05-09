package org.archivekeep.app.ui.components.base.layout

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

val LocalEnableScrollbar = staticCompositionLocalOf { false }

@Composable
fun ScrollableContainer(
    modifier: Modifier,
    scrollbarPadding: PaddingValues,
    scrollbarAdapter: ScrollbarAdapter,
    content: @Composable (modifier: Modifier) -> Unit,
) {
    var columnSize by remember { mutableStateOf<IntSize?>(null) }

    Box(
        modifier = modifier,
    ) {
        content(Modifier.onSizeChanged { size -> columnSize = size })

        if (LocalEnableScrollbar.current) {
            columnSize?.let { size ->
                VerticalScrollbar(
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .height(with(LocalDensity.current) { size.height.toDp() })
                            .padding(scrollbarPadding),
                    adapter = scrollbarAdapter,
                )
            }
        }
    }
}
