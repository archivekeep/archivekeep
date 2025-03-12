package org.archivekeep.app.desktop.ui.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.archivekeep.app.desktop.ui.designsystem.dialog.LabelText
import org.archivekeep.app.desktop.ui.designsystem.input.CheckboxWithText
import org.archivekeep.files.operations.sync.RelocationSyncMode

@Preview
@Composable
fun RelocationSyncModeOptionsPreview() {
    RelocationSyncModeOptions(
        RelocationSyncMode.Move(false, false),
        onRelocationSyncModeChange = {},
    )
}

@Composable
fun RelocationSyncModeOptions(
    relocationSyncMode: RelocationSyncMode,
    onRelocationSyncModeChange: (RelocationSyncMode) -> Unit,
) {
    Column {
        LabelText("Relocations resolution mode:")
        Row(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RadioWithText(
                selected = relocationSyncMode is RelocationSyncMode.Move,
                onClick = { onRelocationSyncModeChange(RelocationSyncMode.Move(false, false)) },
                text = "Allowed",
            )
            RadioWithText(
                selected = relocationSyncMode is RelocationSyncMode.Disabled,
                onClick = { onRelocationSyncModeChange(RelocationSyncMode.Disabled) },
                text = "Disabled",
            )
            RadioWithText(
                selected = relocationSyncMode is RelocationSyncMode.AdditiveDuplicating,
                onClick = { onRelocationSyncModeChange(RelocationSyncMode.AdditiveDuplicating) },
                text = "Duplicate as new",
            )
        }
        if (relocationSyncMode is RelocationSyncMode.Move) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CheckboxWithText(
                    value = relocationSyncMode.allowDuplicateIncrease,
                    onValueChange = { value ->
                        onRelocationSyncModeChange(
                            relocationSyncMode.copy(
                                allowDuplicateIncrease = value,
                            ),
                        )
                    },
                    text = "Allow duplication increase",
                )
                CheckboxWithText(
                    value = relocationSyncMode.allowDuplicateReduction,
                    onValueChange = { value ->
                        onRelocationSyncModeChange(
                            relocationSyncMode.copy(
                                allowDuplicateReduction = value,
                            ),
                        )
                    },
                    text = "Allow duplication reduction",
                )
            }
        }
    }
}

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
