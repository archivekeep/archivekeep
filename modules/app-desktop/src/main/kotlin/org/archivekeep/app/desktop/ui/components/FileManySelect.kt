package org.archivekeep.app.desktop.ui.components

import androidx.compose.foundation.lazy.LazyListScope

fun LazyListScope.fileManySelect(
    label: String,
    state: ManySelectState<String, String, String>,
) {
    itemManySelect(
        label,
        { "All new files ($it)" },
        itemLabelText = { it },
        state,
    )
}
