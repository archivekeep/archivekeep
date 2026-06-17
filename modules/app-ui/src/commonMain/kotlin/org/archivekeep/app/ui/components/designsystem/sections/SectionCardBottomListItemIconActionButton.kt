package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.utils.Action2

@Composable
fun SectionCardBottomListItemIconActionButton(
    action: Action2,
    showIfNotEnabled: Boolean = false,
) {
    if (showIfNotEnabled || action.enabled) {
        SectionCardBottomListItemIconButton(
            icon = action.icon,
            contentDescription = action.title,
            enabled = action.enabled,
            onClick = action.onClick,
        )
    }
}
