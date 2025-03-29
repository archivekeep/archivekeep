package org.archivekeep.app.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.state.ToggleableState

@Composable
fun <O_I, O_S> rememberManySelectWithMergedState(
    allItems: List<O_I>,
    selectionState: MutableState<Set<O_S>>,
): ManySelectState<O_I, O_I, O_S> where O_I : O_S = rememberManySelectWithMergedState(allItems, selectionState, keyMapper = { it })

@Composable
fun <O, K> rememberManySelectWithMergedState(
    allItems: List<O>,
    selectionState: MutableState<Set<K>>,
    keyMapper: (option: O) -> K,
): ManySelectState<O, O, K> where O : K {
    val allKeys =
        remember(allItems, keyMapper) {
            allItems.map(keyMapper)
        }
    val (selectedItems, setSelectedItems) = selectionState

    val onItemChange =
        rememberUpdatedState { item: O, newValue: Boolean ->
            setSelectedItems(
                if (newValue) {
                    selectedItems union setOf(item)
                } else {
                    selectedItems subtract setOf(item)
                },
            )
        }

    val selectAllState =
        remember(selectedItems, allKeys) {
            if (allKeys.none { it in selectedItems }) {
                ToggleableState.Off
            } else if (selectedItems.containsAll(allKeys)) {
                ToggleableState.On
            } else {
                ToggleableState.Indeterminate
            }
        }

    val onSelectAllChange =
        rememberUpdatedState {
            if (selectAllState != ToggleableState.On) {
                setSelectedItems(selectedItems union allKeys.toSet())
            } else {
                setSelectedItems(selectedItems subtract allKeys.toSet())
            }
        }

    return ManySelectState(
        allItems,
        selectedItems,
        onItemChange,
        selectAllState,
        onSelectAllChange,
    )
}
