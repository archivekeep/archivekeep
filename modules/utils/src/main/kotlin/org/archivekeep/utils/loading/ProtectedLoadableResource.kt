package org.archivekeep.utils.loading

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

@Deprecated(
    "Replace with, and/or break down to multiple, Loadable(s) or OptionalLoadable(s), and reduce number of similar generic interfaces.",
)
sealed interface ProtectedLoadableResource<out T, out A> {
    fun asLoadable(): Loadable<T>

    data object Loading : ProtectedLoadableResource<Nothing, Nothing> {
        override fun asLoadable() = Loadable.Loading
    }

    data class Loaded<out T>(
        val value: T,
    ) : ProtectedLoadableResource<T, Nothing> {
        override fun asLoadable(): Loadable<T> = Loadable.Loaded(value)
    }

    data class PendingAuthentication<out A>(
        val authenticationRequest: A,
    ) : ProtectedLoadableResource<Nothing, A> {
        override fun asLoadable(): Loadable<Nothing> = Loadable.Loading
    }

    data class Failed(
        val throwable: Throwable,
    ) : ProtectedLoadableResource<Nothing, Nothing> {
        override fun asLoadable(): Loadable<Nothing> = Loadable.Failed(throwable)
    }
}

fun <T, A> Flow<ProtectedLoadableResource<T, A>>.mapAsLoadable(): Flow<Loadable<T>> =
    this
        .map {
            it.asLoadable()
        }

fun <T, A> Flow<ProtectedLoadableResource<T, A>>.filterLoaded(): Flow<ProtectedLoadableResource.Loaded<T>> =
    this
        .transform {
            if (it is ProtectedLoadableResource.Loaded) {
                emit(it)
            }
        }

suspend fun <T, A> Flow<ProtectedLoadableResource<T, A>>.firstLoadedOrNullOnErrorOrLocked(): T? =
    this
        .transform {
            when (it) {
                is ProtectedLoadableResource.Loaded ->
                    emit(it.value)
                is ProtectedLoadableResource.Loading -> {}
                else ->
                    emit(null)
            }
        }.first()

suspend fun <T, A> Flow<ProtectedLoadableResource<T, A>>.firstLoadedOrThrowOnErrorOrLocked(): T =
    this
        .transform {
            when (it) {
                is ProtectedLoadableResource.Loaded ->
                    emit(it.value)
                is ProtectedLoadableResource.Loading -> {}
                is ProtectedLoadableResource.Failed -> throw RuntimeException(it.throwable)
                is ProtectedLoadableResource.PendingAuthentication<*> -> throw RuntimeException("Not unlocked")
            }
        }.first()
