package org.archivekeep.utils.loading

sealed interface LoadableWithProgress<out T, out Progress> {
    data class Loaded<out T>(
        val value: T,
    ) : LoadableWithProgress<T, Nothing>

    data class Failed(
        val throwable: Throwable,
    ) : LoadableWithProgress<Nothing, Nothing>

    data object Loading : LoadableWithProgress<Nothing, Nothing>

    data class LoadingProgress<out Progress>(
        val progress: Progress,
    ) : LoadableWithProgress<Nothing, Progress>
}
