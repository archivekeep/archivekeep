package org.archivekeep.utils.loading.optional

sealed interface OptionalLoadable<out T> {
    sealed interface LoadingFinished<out T> : OptionalLoadable<T>

    data class LoadedAvailable<out T>(
        val value: T,
    ) : LoadingFinished<T>

    open class NotAvailable(
        val cause: Throwable? = null,
    ) : LoadingFinished<Nothing>

    data class Failed(
        val cause: Throwable,
    ) : LoadingFinished<Nothing>

    data object Loading : OptionalLoadable<Nothing>
}
