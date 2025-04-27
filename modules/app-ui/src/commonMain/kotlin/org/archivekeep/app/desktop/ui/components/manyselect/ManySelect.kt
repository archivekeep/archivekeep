package org.archivekeep.app.desktop.ui.components.manyselect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.state.ToggleableState

data class ManySelectState<O, K, S_K>(
    val allItems: List<O>,
    val selectedItems: Set<S_K>,
    val onItemChangeState: State<(K, Boolean) -> Unit>,
    val selectAllState: ToggleableState,
    val onSelectAllChangeState: State<() -> Unit>,
) where K : S_K {
    val onItemChange by onItemChangeState
    val onSelectAllChange by onSelectAllChangeState
}

@Composable
fun <T> rememberManySelect(
    allItems: List<T>,
    selectionState: MutableState<Set<T>>,
): ManySelectState<T, T, T> = rememberManySelect(allItems, selectionState, keyMapper = { it })

@Composable
fun <O, K> rememberManySelect(
    allItems: List<O>,
    selectionState: MutableState<Set<K>>,
    keyMapper: (option: O) -> K,
): ManySelectState<O, K, K> {
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

    return ManySelectState(
        allItems,
        selectedItems,
        onItemChange,
        selectAllState,
        onSelectAllChange,
    )
}
