package org.archivekeep.app.desktop.ui.designsystem.input

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun CheckboxWithText(
    value: Boolean,
    enabled: Boolean = true,
    role: Role? = Role.Checkbox,
    onValueChange: (Boolean) -> Unit,
    text: String,
    extraItems: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier =
        Modifier
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
) {
    CheckboxWithText(
        value,
        enabled,
        role,
        onValueChange,
        text = { Text(text) },
        extraItems,
        modifier,
    )
}

@Composable
fun CheckboxWithText(
    value: Boolean,
    enabled: Boolean = true,
    role: Role? = Role.Checkbox,
    onValueChange: (Boolean) -> Unit,
    text: @Composable () -> Unit,
    extraItems: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier =
        Modifier
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
) {
    Box(
        modifier =
            Modifier
                .toggleable(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    role = role,
                ),
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                modifier = Modifier.scale(14 / 20f),
                checked = value,
                enabled = enabled,
                // null recommended for accessibility with screenreaders
                onCheckedChange = null,
            )
            Spacer(Modifier.width(2.dp))
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.bodyMedium,
            ) {
                text()
            }
            extraItems?.let {
                Spacer(Modifier.width(4.dp))
                it()
            }
        }
    }
}
