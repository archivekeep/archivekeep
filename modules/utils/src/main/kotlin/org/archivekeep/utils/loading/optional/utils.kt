package org.archivekeep.utils.loading.optional

import org.archivekeep.utils.loading.Loadable

val <T> OptionalLoadable<T>.isLoading: Boolean
    get() = this is OptionalLoadable.Loading

fun <T, R> OptionalLoadable<T>.mapLoadedData(function: (data: T) -> R): OptionalLoadable<R> =
    when (this) {
        is OptionalLoadable.LoadedAvailable ->
            OptionalLoadable.LoadedAvailable(function(this.value))

        is OptionalLoadable.Failed -> this
        OptionalLoadable.Loading -> OptionalLoadable.Loading
        is OptionalLoadable.NotAvailable -> this
    }

fun <T, R> OptionalLoadable<T>.mapIfLoadedOrNull(transform: (item: T) -> R): R? =
    if (this is OptionalLoadable.LoadedAvailable) {
        transform(this.value)
    } else {
        null
    }

fun <T> OptionalLoadable<T>.mapToLoadable(defaultValue: T): Loadable<T> =
    when (this) {
        is OptionalLoadable.Failed -> Loadable.Failed(cause)
        OptionalLoadable.Loading -> Loadable.Loading
        is OptionalLoadable.LoadedAvailable -> Loadable.Loaded(value)
        is OptionalLoadable.NotAvailable -> Loadable.Loaded(defaultValue)
    }

fun <T> OptionalLoadable<T>.mapToLoadable(defaultValue: (OptionalLoadable.NotAvailable) -> Loadable<T>): Loadable<T> =
    when (this) {
        is OptionalLoadable.Failed -> Loadable.Failed(cause)
        OptionalLoadable.Loading -> Loadable.Loading
        is OptionalLoadable.LoadedAvailable -> Loadable.Loaded(value)
        is OptionalLoadable.NotAvailable -> defaultValue(this)
    }
