package org.archivekeep.app.android.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

@Composable
fun StatusBarProtection(heightProvider: () -> Float = calculateGradientHeight()) {
    Canvas(Modifier.fillMaxSize()) {
        val calculatedHeight = heightProvider()

        drawRect(
            color = Color.Black.copy(0.2f),
            size = Size(size.width, calculatedHeight),
        )
    }
}

@Composable
fun calculateGradientHeight(): () -> Float {
    val statusBars = WindowInsets.statusBars
    val density = LocalDensity.current
    return { statusBars.getTop(density).toFloat() }
}
