package org.archivekeep.app.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.state.ToggleableState

data class ManySelectState<O, K>(
    val allItems: Collection<O>,
    val selectedItems: Set<K>,
    val onItemChangeState: State<(K, Boolean) -> Unit>,
    val selectAllState: ToggleableState,
    val onSelectAllChangeState: State<() -> Unit>,
) {
    val onItemChange by onItemChangeState
    val onSelectAllChange by onSelectAllChangeState
}

@Composable
fun <T> rememberManySelect(
    allItems: Collection<T>,
    selectionState: MutableState<Set<T>>,
): ManySelectState<T, T> = rememberManySelect(allItems, selectionState, keyMapper = { it })

@Composable
fun <O, K> rememberManySelect(
    allItems: Collection<O>,
    selectionState: MutableState<Set<K>>,
    keyMapper: (option: O) -> K,
): ManySelectState<O, K> {
    val allKeys =
        remember(allItems, keyMapper) {
            allItems.map(keyMapper)
        }
    val (selectedItems, setSelectedItems) = selectionState

    val onItemChange =
        rememberUpdatedState { item: K, newValue: Boolean ->
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
            if (selectedItems.isEmpty()) {
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
                setSelectedItems(allKeys.toSet())
            } else {
                setSelectedItems(emptySet())
            }
        }

    return ManySelectState<O, K>(
        allItems,
        selectedItems,
        onItemChange,
        selectAllState,
        onSelectAllChange,
    )
}
