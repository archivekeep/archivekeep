package org.archivekeep.app.ui.components.base.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.max

@Composable
fun HeightKeepingBox(content: @Composable BoxScope.() -> Unit) {
    // TODO: add animated collapse after a delay

    val maxHeight = remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .onSizeChanged { maxHeight.value = max(it.height, maxHeight.value) }
            .heightIn(min = with(LocalDensity.current) { maxHeight.value.toDp() }),
        content = content
    )
}
