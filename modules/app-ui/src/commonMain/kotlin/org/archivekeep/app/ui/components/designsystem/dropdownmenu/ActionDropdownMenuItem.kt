package org.archivekeep.app.ui.components.designsystem.dropdownmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.utils.Action
import org.archivekeep.utils.loading.Loadable

@Composable
fun ActionDropdownMenuItem(
    action: Loadable<Action>,
    onClose: () -> Unit,
) {
    (action as? Loadable.Loaded)?.let { action ->
        ActionDropdownMenuItem(action.value, onClose)
    }
}

@Composable
fun ActionDropdownMenuItem(
    action: Action,
    onClose: () -> Unit,
) {
    if (action.isAvailable) {
        DropdownMenuItem(
            onClick = {
                action.onLaunch()
                onClose()
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (action.icon != null) {
                        Icon(
                            action.icon,
                            contentDescription = action.title,
                            modifier = Modifier.padding(end = 8.dp).size(14.dp),
                        )
                    }
                    Text(action.title)
                }
            },
        )
    }
}
