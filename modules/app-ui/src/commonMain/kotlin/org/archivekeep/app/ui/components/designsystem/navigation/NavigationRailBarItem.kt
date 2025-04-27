package org.archivekeep.app.ui.components.designsystem.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NavigationRailBarItem(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    contentColor: Color,
    selectedColor: Color,
    modifier: Modifier,
) {
    val backgroundColor = if (selected) selectedColor else Color.Transparent

    Surface(
        selected = selected,
        onClick = onClick,
        contentColor = contentColor,
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.let { if (selected) it else it.alpha(0.8f) },
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
        ) {
            Column(
                modifier = modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    text,
                    modifier = Modifier.size(24.dp).padding(bottom = 4.dp),
                )
                Text(text, fontSize = 9.sp, lineHeight = 9.sp)
            }
        }
    }
}
