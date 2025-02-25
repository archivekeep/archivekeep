package org.archivekeep.utils.loading

sealed interface Loadable<out T> {
    data class Loaded<out T>(
        val value: T,
    ) : Loadable<T>

    data class Failed(
        val throwable: Throwable,
    ) : Loadable<Nothing>

    data object Loading : Loadable<Nothing>
}
