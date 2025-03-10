package org.archivekeep.app.desktop.ui.designsystem.input

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp

@Composable
fun TriStateCheckboxWithText(
    state: ToggleableState,
    enabled: Boolean = true,
    role: Role? = Role.Checkbox,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier =
        Modifier
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 8.dp),
) {
    Row(
        modifier =
            modifier
                .triStateToggleable(
                    state = state,
                    onClick = onClick,
                    enabled = enabled,
                    role = role,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(
            modifier = Modifier.scale(14f / 20f),
            state = state,
            enabled = enabled,
            // null recommended for accessibility with screenreaders
            onClick = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}
