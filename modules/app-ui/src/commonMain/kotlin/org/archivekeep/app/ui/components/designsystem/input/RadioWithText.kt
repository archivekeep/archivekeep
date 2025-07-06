package org.archivekeep.app.ui.components.designsystem.input

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun RadioWithText(
    selected: Boolean,
    enabled: Boolean = true,
    role: Role? = Role.RadioButton,
    onClick: () -> Unit,
    text: String,
) {
    Row(
        Modifier
            .defaultMinSize(minHeight = 40.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = role,
            ).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
