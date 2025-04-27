package org.archivekeep.app.desktop.ui.components.manyselect

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.archivekeep.utils.collections.limitSize
import kotlin.math.max

private const val itemsToConsider = 500
private const val itemsToMeasure = 30

data class ManySelectForRender<O, K : S_K, S_K>(
    val state: ManySelectState<O, K, S_K>,
    val guessedWidth: Dp,
    val render: LazyListScope.() -> Unit,
)

@Composable
fun <I> rememberManySelectForRender(
    allItems: List<I>,
    selectionState: MutableState<Set<I>>,
    label: String,
    allItemsLabel: (count: Int) -> String,
    itemLabelText: (item: I) -> String,
): ManySelectForRender<I, I, I> {
    val state = rememberManySelect(allItems, selectionState)

    return rememberManySelectForRenderFromState(
        state,
        label,
        allItemsLabel = allItemsLabel,
        itemLabelText = itemLabelText,
    )
}

@Composable
fun <O_I, O_S> rememberManySelectForRenderFromState(
    state: ManySelectState<O_I, O_I, O_S>,
    label: String,
    allItemsLabel: (count: Int) -> String,
    itemLabelText: (item: O_I) -> String,
): ManySelectForRender<O_I, O_I, O_S> where O_I : O_S {
    val guessedWidth = guessAndRememberManySelectWidth(state.allItems, allItemsLabel, itemLabelText)

    return ManySelectForRender(
        state,
        guessedWidth,
        render = {
            itemManySelect(
                label,
                allItemsLabel = allItemsLabel,
                itemLabelText = itemLabelText,
                state = state,
            )
        },
    )
}

@Composable
fun <O_I, O_S> rememberManySelectForRenderFromStateAnnotated(
    state: ManySelectState<O_I, O_I, O_S>,
    label: String,
    allItemsLabel: (count: Int) -> String,
    itemAnnotatedLabel: (item: O_I) -> AnnotatedString,
): ManySelectForRender<O_I, O_I, O_S> where O_I : O_S {
    val guessedWidth =
        guessAndRememberManySelectWidthAnnotated(state.allItems, allItemsLabel, itemAnnotatedLabel)

    return ManySelectForRender(
        state,
        guessedWidth,
        render = {
            itemManySelect(
                label,
                allItemsLabel = allItemsLabel,
                itemLabel = { Text(itemAnnotatedLabel(it)) },
                state = state,
            )
        },
    )
}

@Composable
fun <I> guessAndRememberManySelectWidth(
    allItems: List<I>,
    allItemsLabel: (size: Int) -> String,
    itemLabelText: (item: I) -> String,
): Dp {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodyMedium

    val width =
        remember(allItems, textStyle) {
            max(
                textMeasurer.measure(allItemsLabel(allItems.size), style = textStyle).size.width,
                allItems
                    .limitSize(itemsToConsider)
                    .map { itemLabelText(it) }
                    .sortedByDescending { it.length }
                    .limitSize(itemsToMeasure)
                    .maxOfOrNull { textMeasurer.measure(it, style = textStyle).size.width }
                    ?: 0,
            )
        }

    return with(LocalDensity.current) { width.toDp() } + 60.dp
}

@Composable
fun <I> guessAndRememberManySelectWidthAnnotated(
    allItems: List<I>,
    allItemsLabel: (size: Int) -> String,
    itemLabelAnnotatedString: (item: I) -> AnnotatedString,
): Dp {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodyMedium

    val width =
        remember(allItems, textStyle) {
            max(
                textMeasurer.measure(allItemsLabel(allItems.size), style = textStyle).size.width,
                allItems
                    .limitSize(itemsToConsider)
                    .map { itemLabelAnnotatedString(it) }
                    .sortedByDescending { it.length }
                    .limitSize(itemsToMeasure)
                    .maxOfOrNull { textMeasurer.measure(it, style = textStyle).size.width }
                    ?: 0,
            )
        }

    return with(LocalDensity.current) { width.toDp() } + 60.dp
}
