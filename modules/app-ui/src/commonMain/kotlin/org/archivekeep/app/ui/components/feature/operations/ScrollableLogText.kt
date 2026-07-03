package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.base.layout.IntrinsicSizeWrapperLayout
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumn
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.dialog.labelTextStyle
import org.archivekeep.utils.collections.limitSize
import kotlin.math.max

private const val itemsToConsider = 500
private const val itemsToMeasure = 30

@Deprecated(
    "Use overload accepting a list of text lines. This is more expensive, because splits text into individual lines.",
    ReplaceWith("ScrollableLogTextInDialog(textLines)"),
)
@Composable
fun ScrollableLogTextInDialog(text: String) {
    val textLines = remember(text) { text.split("\n") }

    ScrollableLogTextInDialog(textLines)
}

@Composable
fun ScrollableLogTextInDialog(textLines: List<String>) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodyMedium

    val width =
        remember(textLines, textStyle) {
            max(
                textMeasurer.measure("Log", style = labelTextStyle).size.width,
                textLines
                    .limitSize(itemsToConsider)
                    .map { it }
                    .sortedByDescending { it.length }
                    .limitSize(itemsToMeasure)
                    .maxOfOrNull { textMeasurer.measure(it, style = textStyle).size.width }
                    ?: 0,
            )
        }

    val guessedWidth = with(LocalDensity.current) { width.toDp() } + 60.dp

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color.Black.copy(0.05f),
    ) {
        SelectionContainer(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 10.dp)) {
            IntrinsicSizeWrapperLayout(
                minIntrinsicWidth = guessedWidth,
                maxIntrinsicWidth = guessedWidth,
            ) {
                ScrollableLazyColumn {
                    item { LabelText("Log") }
                    item { Spacer(Modifier.height(4.dp)) }

                    items(textLines) { text ->
                        Text(text.trimEnd())
                    }
                }
            }
        }
    }
}
