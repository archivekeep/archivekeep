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
import org.archivekeep.ui.resources.Res
import org.archivekeep.ui.resources.sync_allow_duplication_increase
import org.archivekeep.ui.resources.sync_allow_duplication_reduction
import org.archivekeep.ui.resources.sync_relocation_allowed
import org.archivekeep.ui.resources.sync_relocation_disabled
import org.archivekeep.ui.resources.sync_relocation_duplicate_as_new
import org.archivekeep.ui.resources.sync_relocation_mode_label
import org.jetbrains.compose.resources.stringResource

@Composable
fun RelocationSyncModeOptions(
    relocationSyncMode: RelocationSyncMode,
    onRelocationSyncModeChange: (RelocationSyncMode) -> Unit,
) {
    Column {
        LabelText(stringResource(Res.string.sync_relocation_mode_label))
        Row(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RadioWithText(
                selected = relocationSyncMode is RelocationSyncMode.Move,
                onClick = { onRelocationSyncModeChange(RelocationSyncMode.Move(false, false)) },
                text = stringResource(Res.string.sync_relocation_allowed),
            )
            RadioWithText(
                selected = relocationSyncMode is RelocationSyncMode.Disabled,
                onClick = { onRelocationSyncModeChange(RelocationSyncMode.Disabled) },
                text = stringResource(Res.string.sync_relocation_disabled),
            )
            RadioWithText(
                selected = relocationSyncMode is RelocationSyncMode.AdditiveDuplicating,
                onClick = { onRelocationSyncModeChange(RelocationSyncMode.AdditiveDuplicating) },
                text = stringResource(Res.string.sync_relocation_duplicate_as_new),
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
                    text = stringResource(Res.string.sync_allow_duplication_increase),
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
                    text = stringResource(Res.string.sync_allow_duplication_reduction),
                )
            }
        }
    }
}
