package org.archivekeep.app.desktop.ui.designsystem.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors.Companion.navigationRailColor

@Composable
fun NavigationRail(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .zIndex(10f)
            .background(navigationRailColor)
            .padding(6.dp)
            .width(IntrinsicSize.Max),
        content = content,
    )
}
