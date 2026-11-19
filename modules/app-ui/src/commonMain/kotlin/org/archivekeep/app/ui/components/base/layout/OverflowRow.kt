package org.archivekeep.app.ui.components.base.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

@Composable
fun <T> OverflowRow(
    items: List<T>,
    spacing: Dp,
    contentAlignment: Alignment = Alignment.CenterStart,
    itemContent: @Composable (T) -> Unit,
    overflowContent: @Composable (List<T>) -> Unit,
) {
    if (items.isEmpty()) return

    val spacingPx = with(LocalDensity.current) { spacing.roundToPx() }

    Box(contentAlignment = contentAlignment) {
        SubcomposeLayout { constraints ->
            val availableWidth = constraints.maxWidth

            val itemMeasurables = subcompose("items") {
                items.forEach { item ->
                    itemContent(item)
                }
            }
            val itemPlaceables = itemMeasurables.map { measurable ->
                measurable.measure(Constraints(maxWidth = availableWidth, maxHeight = constraints.maxHeight))
            }

            if (itemPlaceables.isEmpty()) {
                return@SubcomposeLayout layout(availableWidth, 0) {}
            }

            val allItemsWidth = itemPlaceables.fold(0) { acc, p -> acc + p.width + spacingPx } - spacingPx

            if (allItemsWidth <= availableWidth) {
                val rowHeight = itemPlaceables.maxOf { it.height }
                var x = 0
                return@SubcomposeLayout layout(availableWidth, rowHeight) {
                    for (placeable in itemPlaceables) {
                        placeable.place(x, 0)
                        x += placeable.width + spacingPx
                    }
                }
            }

            val overflowEstimate = 50
            val estimatedMaxItemsWidth = availableWidth - overflowEstimate - spacingPx
            val hiddenStartIndex = computeVisibleCount(
                placeables = itemPlaceables,
                spacing = spacingPx,
                maxWidth = estimatedMaxItemsWidth,
            ).coerceAtLeast(1)

            val hiddenItems = items.subList(hiddenStartIndex, items.size)

            val overflowMeasurables = subcompose("overflow") {
                overflowContent(hiddenItems)
            }

            val overflowPlaceable: Placeable? = overflowMeasurables.firstOrNull()
                ?.measure(Constraints(maxWidth = availableWidth, maxHeight = constraints.maxHeight))

            if (overflowPlaceable == null) {
                val rowHeight = itemPlaceables[0].height
                return@SubcomposeLayout layout(availableWidth, rowHeight) {
                    itemPlaceables[0].place(0, 0)
                }
            }

            val maxItemsWidth = availableWidth - overflowPlaceable.width - spacingPx
            val visibleCount = computeVisibleCount(
                placeables = itemPlaceables,
                spacing = spacingPx,
                maxWidth = maxItemsWidth,
            ).coerceAtLeast(1)

            val rowHeight = itemPlaceables.maxOf { it.height }.coerceAtLeast(overflowPlaceable.height)
            val overflowY = (rowHeight - overflowPlaceable.height) / 2
            var x = 0
            layout(availableWidth, rowHeight) {
                for (i in 0 until visibleCount) {
                    itemPlaceables[i].place(x, 0)
                    x += itemPlaceables[i].width + spacingPx
                }
                overflowPlaceable.place(x, overflowY)
            }
        }
    }
}

private fun computeVisibleCount(
    placeables: List<Placeable>,
    spacing: Int,
    maxWidth: Int,
): Int {
    var totalWidth = 0
    var count = 0
    for (i in placeables.indices) {
        val itemWidth = placeables[i].width
        val neededWidth = if (i == 0) itemWidth else itemWidth + spacing
        if (totalWidth + neededWidth <= maxWidth) {
            totalWidth += neededWidth
            count = i + 1
        } else {
            break
        }
    }
    return count
}
