package org.archivekeep.app.ui.components.feature.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.core.persistence.platform.demo.fullComplexPreset

val presets =
    listOf(
        fullComplexPreset,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetSettings() {
    var expanded by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf(presets[0]) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Demo", style = MaterialTheme.typography.titleMedium)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            },
        ) {
            TextField(
                value = selectedPreset.title,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { TrailingIcon(expanded = expanded) },
                label = {
                    Text("Preset")
                },
                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable).heightIn(min = 4.dp),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                presets.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text = item.title) },
                        onClick = {
                            selectedPreset = item
                            expanded = false
                            println("selected")
                        },
                    )
                }
            }
        }
    }
}
