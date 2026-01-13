package org.archivekeep.utils.loading

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

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

inline fun <T> MutableStateFlow<Loadable<T>>.produceAndGet(producer: () -> T): T {
    try {
        return producer().also { value = Loadable.Loaded(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        value = Loadable.Failed(e)
        throw e
    }
}
