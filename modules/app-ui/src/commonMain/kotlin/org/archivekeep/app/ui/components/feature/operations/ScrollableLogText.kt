package org.archivekeep.app.ui.components.feature.operations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.archivekeep.app.ui.components.base.layout.ScrollableLazyColumnWithGuessedWidth
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.dialog.labelTextStyle
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.common_autoscroll
import org.archivekeep.ui.resources.common_log
import org.archivekeep.utils.collections.limitSize
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max

private const val itemsToConsider = 500
private const val itemsToMeasure = 30

@Composable
fun ScrollableLogTextInDialog(
    textLines: List<String>,
    autoScrollEnabled: Boolean = true,
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodyMedium
    val logLabel = stringResource(Res.string.common_log)

    val width =
        remember(textLines, textStyle, logLabel) {
            max(
                textMeasurer.measure(logLabel, style = labelTextStyle).size.width,
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

    val listState = rememberLazyListState()
    var autoScrollOn by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    if (autoScrollEnabled) {
        LaunchedEffect(autoScrollOn, textLines.size) {
            if (autoScrollOn) {
                listState.scrollToItem(Int.MAX_VALUE)
            }
        }

        var previousIndex by remember { mutableIntStateOf(0) }
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }.collectLatest { index ->
                if (index < previousIndex) {
                    autoScrollOn = false
                }
                previousIndex = index
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color.Black.copy(0.05f),
    ) {
        Box {
            SelectionContainer(Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 10.dp)) {
                ScrollableLazyColumnWithGuessedWidth(
                    guessedWidth = guessedWidth,
                    enableHorizontalScroll = true,
                    state = listState,
                ) {
                    item { LabelText(stringResource(Res.string.common_log)) }
                    item { Spacer(Modifier.height(4.dp)) }

                    items(textLines) { text ->
                        Text(text.trimEnd())
                    }
                }
            }

            if (autoScrollEnabled && !autoScrollOn) {
                ScrollToBottomOverlayButton(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    onClick = {
                        autoScrollOn = true
                        scope.launch {
                            listState.scrollToItem(Int.MAX_VALUE)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ScrollToBottomOverlayButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val alpha by animateFloatAsState(
        targetValue = if (isHovered) 1f else 0.35f,
        label = "scrollButtonAlpha",
    )

    Surface(
        modifier = modifier.alpha(alpha),
        shape = MaterialTheme.shapes.small,
        color = Color.LightGray,
        contentColor = Color.Black,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Notes,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(text = stringResource(Res.string.common_autoscroll))
        }
    }
}
