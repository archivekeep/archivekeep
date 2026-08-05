package org.archivekeep.app.ui.components.designsystem.sections

import androidx.compose.runtime.Composable
import org.archivekeep.app.ui.utils.Action

@Composable
fun SectionCardBottomListItemIconActionButton(action: Action) {
    if (action.isPending && action.icon != null) {
        SectionCardBottomListItemIconButton(
            icon = action.icon,
            contentDescription = action.title,
            onClick = action.onLaunch,
        )
    }
}
