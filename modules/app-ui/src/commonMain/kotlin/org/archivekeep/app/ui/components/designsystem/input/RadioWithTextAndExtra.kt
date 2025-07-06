package org.archivekeep.app.ui.components.designsystem.input

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun RadioWithTextAndExtra(
    selected: Boolean,
    enabled: Boolean = true,
    role: Role? = Role.RadioButton,
    onClick: () -> Unit,
    text: String,
    extra: @Composable () -> Unit,
) {
    var radioWidth by remember { mutableStateOf(0) }

    Column {
        Row(
            Modifier
                .defaultMinSize(minHeight = 40.dp)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    onClick = onClick,
                    role = role,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier =
                    Modifier
                        .onSizeChanged {
                            radioWidth = it.width
                        }.padding(horizontal = 8.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Box(Modifier.padding(start = with(LocalDensity.current) { radioWidth.toDp() })) {
            extra()
        }
    }
}
