package org.archivekeep.app.desktop.ui.designsystem.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors.Companion.navigationRailColor

@Composable
fun NavigationRail(content: @Composable ColumnScope.() -> Unit) {
    val insets = WindowInsets.safeDrawing
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    Column(
        Modifier
            .zIndex(10f)
            .background(navigationRailColor)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Start),
            ).padding(
                (6.dp - (with(density) { insets.getLeft(density, layoutDirection).toDp() })).coerceAtLeast(0.dp),
                end = 6.dp,
                top = 6.dp,
                bottom = 6.dp,
            ).width(IntrinsicSize.Max),
        content = content,
    )
}
