package org.archivekeep.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface Loadable<out T> {
    data class Loaded<out T>(
        val value: T,
    ) : Loadable<T>

    data class Failed(
        val throwable: Throwable,
    ) : Loadable<Nothing>

    data object Loading : Loadable<Nothing>
}

val <T> Loadable<T>.isLoading
    get() = this is Loadable.Loading

fun <T, R> Loadable<T>.mapIfLoadedOrDefault(
    default: R,
    transform: (item: T) -> R,
): R =
    if (this is Loadable.Loaded) {
        transform(this.value)
    } else {
        default
    }

fun <T, R> Loadable<T>.mapIfLoadedOrNull(transform: (item: T) -> R): R? =
    if (this is Loadable.Loaded) {
        transform(this.value)
    } else {
        null
    }

fun <T> anyHasLoadingFlow(
    items: List<T>,
    transform: (item: T) -> Flow<Loadable<*>>,
): Flow<Boolean> =
    safeCombine(items.map { item -> transform(item).map { it.isLoading } }) { itemsLoadings ->
        itemsLoadings.any { it }
    }
