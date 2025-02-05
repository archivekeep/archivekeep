package org.archivekeep.app.desktop.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

@Composable
fun ColumnScope.FileManySelect(
    label: String,
    allFiles: List<String>,
    selectedFilenames: MutableState<Set<String>>,
) {
    ItemManySelect(
        label,
        { "All new files ($it)" },
        { it },
        allFiles,
        selectedFilenames,
    )
}
