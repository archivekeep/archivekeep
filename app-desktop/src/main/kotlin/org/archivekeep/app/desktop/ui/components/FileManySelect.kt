package org.archivekeep.app.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.archivekeep.app.desktop.ui.designsystem.input.CheckboxWithText
import org.archivekeep.app.desktop.ui.designsystem.input.TriStateCheckboxWithText

@Composable
fun ColumnScope.FileManySelect(
    allFiles: List<String>,
    selectedFilenames: MutableState<Set<String>>,
) {
    val fileManySelect = rememberManySelect(allFiles, selectedFilenames)

    Text("Files to add and push:", fontSize = 10.sp)

    TriStateCheckboxWithText(
        fileManySelect.selectAllState,
        text = "All new files (${fileManySelect.allItems.size})",
        onClick = fileManySelect.onSelectAllChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 32.dp),
    )

    Box(
        modifier =
            Modifier
                .background(Color(0xFFF9F9F9))
                .verticalScroll(rememberScrollState())
                .weight(weight = 1f, fill = false),
    ) {
        Column {
            fileManySelect.allItems.forEach { filename ->
                CheckboxWithText(
                    fileManySelect.selectedItems.contains(filename),
                    text = filename,
                    onValueChange = {
                        fileManySelect.onItemChange(filename, it)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 24.dp),
                )
            }
        }
    }
}
