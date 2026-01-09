package org.archivekeep.app.ui.components.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.ui.components.designsystem.dialog.LabelText
import org.archivekeep.app.ui.components.designsystem.input.CheckboxWithText
import org.archivekeep.app.ui.components.designsystem.input.RadioWithText
import org.archivekeep.files.procedures.sync.discovery.RelocationSyncMode

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
