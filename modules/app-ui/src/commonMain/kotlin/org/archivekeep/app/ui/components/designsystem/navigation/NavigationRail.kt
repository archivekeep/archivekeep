package org.archivekeep.app.ui.components.designsystem.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.archivekeep.app.ui.components.designsystem.theme.CColors.Companion.navigationRailColor

@Composable
fun NavigationRail(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .zIndex(10f)
            .background(navigationRailColor)
            .consumeWindowInsets(PaddingValues(start = 6.dp))
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
            ).padding(6.dp)
            .width(IntrinsicSize.Max),
        content = content,
    )
}
