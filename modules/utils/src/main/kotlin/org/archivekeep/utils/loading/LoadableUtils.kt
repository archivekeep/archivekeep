package org.archivekeep.utils.loading

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
