package org.archivekeep.app.desktop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.archivekeep.app.desktop.ui.designsystem.dialog.LabelText
import org.archivekeep.app.desktop.ui.designsystem.input.CheckboxWithText
import org.archivekeep.app.desktop.ui.designsystem.input.TriStateCheckboxWithText

@Composable
fun <I> ColumnScope.ItemManySelect(
    label: String,
    allItemsLabel: (count: Int) -> String,
    itemLabelText: (item: I) -> String,
    allItems: List<I>,
    selectedItems: MutableState<Set<I>>,
) {
    val state = rememberManySelect(allItems, selectedItems)

    ItemManySelect(
        label,
        allItemsLabel,
        itemLabelText = itemLabelText,
        state = state,
    )
}

@Composable
fun <I, I_S> ColumnScope.ItemManySelect(
    label: String,
    allItemsLabel: (count: Int) -> String,
    itemLabelText: (item: I) -> String,
    state: ManySelectState<I, I, I_S>,
) where I : I_S {
    ItemManySelect(
        label,
        allItemsLabel,
        itemLabel = { Text(itemLabelText(it)) },
        state = state,
    )
}

@Composable
fun <I, I_S> ColumnScope.ItemManySelect(
    label: String,
    allItemsLabel: (count: Int) -> String,
    itemLabel: @Composable (item: I) -> Unit,
    state: ManySelectState<I, I, I_S>,
) where I : I_S {
    LabelText(label)

    TriStateCheckboxWithText(
        state.selectAllState,
        text = allItemsLabel(state.allItems.size),
        onClick = state.onSelectAllChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 32.dp),
    )

    Column {
        state.allItems.forEach { item ->
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
}
