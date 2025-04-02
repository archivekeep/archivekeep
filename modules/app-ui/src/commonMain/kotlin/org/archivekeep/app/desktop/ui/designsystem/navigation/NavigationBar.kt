package org.archivekeep.app.desktop.ui.designsystem.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.archivekeep.app.desktop.ui.designsystem.styles.CColors.Companion.navigationRailColor

@Composable
fun NavigationBar(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .zIndex(10f)
            .background(navigationRailColor)
            .padding(6.dp)
            .height(IntrinsicSize.Min)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        content = content,
    )
}
