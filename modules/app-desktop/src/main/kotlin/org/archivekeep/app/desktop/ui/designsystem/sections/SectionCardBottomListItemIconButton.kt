package org.archivekeep.app.desktop.ui.designsystem.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SectionCardBottomListItemIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
        modifier = if (enabled) Modifier else Modifier.alpha(0.4f),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.padding(6.dp).size(16.dp),
        )
    }
}
