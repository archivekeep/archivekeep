package org.archivekeep.app.ui.components.base.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun IntrinsicSizeWrapperLayout(
    minIntrinsicWidth: Dp = 0.dp,
    maxIntrinsicWidth: Dp = 10000.dp,
    minIntrinsicHeight: Dp = 0.dp,
    maxIntrinsicHeight: Dp = 10000.dp,
    content: @Composable () -> Unit,
) {
    with(LocalDensity.current) {
        IntrinsicSizeWrapperLayout(
            content = content,
            minIntrinsicWidth = minIntrinsicWidth.roundToPx(),
            maxIntrinsicWidth = maxIntrinsicWidth.roundToPx(),
            minIntrinsicHeight = minIntrinsicHeight.roundToPx(),
            maxIntrinsicHeight = maxIntrinsicHeight.roundToPx(),
        )
    }
}

@Composable
fun IntrinsicSizeWrapperLayout(
    minIntrinsicWidth: Int = 0,
    maxIntrinsicWidth: Int = 10000,
    minIntrinsicHeight: Int = 0,
    maxIntrinsicHeight: Int = 10000,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        measurePolicy =
            object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints,
                ): MeasureResult {
                    val placeable = measurables.first().measure(constraints)

                    return layout(constraints.maxWidth, placeable.height) {
                        placeable.place(0, 0)
                    }
                }

                override fun IntrinsicMeasureScope.minIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int,
                ): Int = minIntrinsicWidth

                override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int,
                ): Int = maxIntrinsicWidth

                override fun IntrinsicMeasureScope.minIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int,
                ): Int = minIntrinsicHeight

                override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int,
                ): Int = maxIntrinsicHeight
            },
    )
}
