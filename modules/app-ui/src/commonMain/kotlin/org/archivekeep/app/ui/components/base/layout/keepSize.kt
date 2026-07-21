package org.archivekeep.app.ui.components.base.layout

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.max

@Composable
fun Modifier.keepSize(
    keepWidth: Boolean = true,
    keepHeight: Boolean = true,
): Modifier {
    val largestWidth = remember(keepWidth) { mutableIntStateOf(0) }
    val largestHeight = remember(keepHeight) { mutableIntStateOf(0) }

    val density = LocalDensity.current

    return this
        .onSizeChanged {
            if (keepWidth) {
                largestWidth.intValue = max(it.width, largestWidth.intValue)
            }
            if (keepHeight) {
                largestHeight.intValue = max(it.height, largestHeight.intValue)
            }
        }.sizeIn(
            minWidth = with(density) { largestWidth.intValue.toDp() },
            minHeight = with(density) { largestHeight.intValue.toDp() },
        )
}
