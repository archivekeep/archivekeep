package org.archivekeep.app.desktop.ui.components.manyselect

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.desktop.ui.designsystem.dialog.LabelText
import org.archivekeep.app.desktop.ui.designsystem.input.CheckboxWithText
import org.archivekeep.app.desktop.ui.designsystem.input.TriStateCheckboxWithText

private val ItemManySelectCheckboxContentType = "ItemManySelect -> Checkbox"

fun <I, I_S> LazyListScope.itemManySelect(
    label: String,
    allItemsLabel: (count: Int) -> String,
    itemLabelText: (item: I) -> String,
    state: ManySelectState<I, I, I_S>,
) where I : I_S {
    itemManySelect(
        label,
        allItemsLabel,
        itemLabel = { Text(itemLabelText(it)) },
        state = state,
    )
}

fun <I, I_S> LazyListScope.itemManySelect(
    label: String,
    allItemsLabel: (count: Int) -> String,
    itemLabel: @Composable (item: I) -> Unit,
    state: ManySelectState<I, I, I_S>,
) where I : I_S {
    item {
        LabelText(label)
    }

    item {
        TriStateCheckboxWithText(
            state.selectAllState,
            text = allItemsLabel(state.allItems.size),
            onClick = state.onSelectAllChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 32.dp),
        )
    }

    items(
        state.allItems,
        contentType = { ItemManySelectCheckboxContentType },
    ) { item ->
        CheckboxWithText(
            state.selectedItems.contains(item),
            text = { itemLabel(item) },
            onValueChange = {
                state.onItemChange(item, it)
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 24.dp),
        )
    }
}
