package org.archivekeep.app.core.utils.generics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import org.archivekeep.utils.loading.Loadable

sealed interface OptionalLoadable<out T> {
    sealed interface LoadingFinished<out T> : OptionalLoadable<T>

    data class LoadedAvailable<out T>(
        val value: T,
    ) : LoadingFinished<T>

    data class NotAvailable(
        val cause: Throwable? = null,
    ) : LoadingFinished<Nothing>

    data class Failed(
        val cause: Throwable,
    ) : OptionalLoadable<Nothing>

    data object Loading : OptionalLoadable<Nothing>
}

val <T> OptionalLoadable<T>.isLoading: Boolean
    get() = this is OptionalLoadable.Loading

fun <T, R> OptionalLoadable<T>.mapLoadedData(function: (data: T) -> R): OptionalLoadable<R> =
    when (this) {
        is OptionalLoadable.LoadedAvailable ->
            OptionalLoadable.LoadedAvailable(function(this.value))

        is OptionalLoadable.Failed -> OptionalLoadable.Failed(this.cause)
        OptionalLoadable.Loading -> OptionalLoadable.Loading
        is OptionalLoadable.NotAvailable -> OptionalLoadable.NotAvailable(this.cause)
    }

fun <T, R> Flow<OptionalLoadable<T>>.mapLoadedData(function: suspend (data: T) -> R): Flow<OptionalLoadable<R>> =
    this
        .map {
            when (it) {
                is OptionalLoadable.LoadedAvailable ->
                    OptionalLoadable.LoadedAvailable(function(it.value))

                is OptionalLoadable.Failed -> OptionalLoadable.Failed(it.cause)
                OptionalLoadable.Loading -> OptionalLoadable.Loading
                is OptionalLoadable.NotAvailable -> OptionalLoadable.NotAvailable(it.cause)
            }
        }.autoCatch()

fun <T, R> Flow<OptionalLoadable<T>>.mapLoaded(transform: suspend (data: T) -> OptionalLoadable<R>): Flow<OptionalLoadable<R>> =
    this
        .map {
            when (it) {
                is OptionalLoadable.LoadedAvailable -> transform(it.value)
                is OptionalLoadable.Failed -> OptionalLoadable.Failed(it.cause)
                OptionalLoadable.Loading -> OptionalLoadable.Loading
                is OptionalLoadable.NotAvailable -> OptionalLoadable.NotAvailable(it.cause)
            }
        }.autoCatch()

@ExperimentalCoroutinesApi
fun <T, R> Flow<OptionalLoadable<T>>.flatMapLatestLoadedData(
    onNotAvailable: suspend (OptionalLoadable.NotAvailable) -> Flow<OptionalLoadable<R>> = { flowOf(it) },
    onLoading: suspend () -> Flow<OptionalLoadable<R>> = { flowOf(OptionalLoadable.Loading) },
    onFailed: suspend (OptionalLoadable.Failed) -> Flow<OptionalLoadable<R>> = {
        it.cause.printStackTrace()
        flowOf(OptionalLoadable.Failed(it.cause))
    },
    transform: suspend (data: T) -> Flow<OptionalLoadable<R>>,
): Flow<OptionalLoadable<R>> =
    this
        .flatMapLatest {
            when (it) {
                is OptionalLoadable.Failed -> onFailed(it)
                is OptionalLoadable.NotAvailable -> onNotAvailable(it)
                is OptionalLoadable.LoadedAvailable -> transform(it.value)
                OptionalLoadable.Loading -> onLoading()
            }
        }

fun <T> Flow<Loadable<T>>.mapToOptionalLoadable(): Flow<OptionalLoadable<T>> = this.mapLoadedDataAsOptional { it }

fun <T, R> Flow<Loadable<T>>.mapLoadedDataAsOptional(function: suspend (data: T) -> R?): Flow<OptionalLoadable<R>> =
    this
        .map {
            when (it) {
                is Loadable.Loaded ->
                    function(it.value).let { result ->
                        if (result != null) {
                            OptionalLoadable.LoadedAvailable(result)
                        } else {
                            OptionalLoadable.NotAvailable()
                        }
                    }
                is Loadable.Failed -> OptionalLoadable.Failed(it.throwable)
                Loadable.Loading -> OptionalLoadable.Loading
            }
        }.autoCatch()

fun <T, R> OptionalLoadable<T>.mapIfLoadedOrNull(transform: (item: T) -> R): R? =
    if (this is OptionalLoadable.LoadedAvailable) {
        transform(this.value)
    } else {
        null
    }

inline fun <T, R> Flow<T>.mapToOptionalLoadable(
    message: String? = null,
    crossinline transform: suspend (value: T) -> R?,
): Flow<OptionalLoadable<R>> =
    this
        .map<T, OptionalLoadable<R>> {
            transform(it).let { result ->
                if (result != null) {
                    OptionalLoadable.LoadedAvailable(result)
                } else {
                    OptionalLoadable.NotAvailable()
                }
            }
        }.autoCatch(message)

fun <T> Flow<OptionalLoadable<T>>.autoCatch(message: String? = null): Flow<OptionalLoadable<T>> =
    this.catch { cause ->
        emit(
            OptionalLoadable.Failed(
                RuntimeException(
                    "${message ?: "mapToOptionalLoadable"}: ${cause.message ?: cause.toString()}",
                    cause,
                ),
            ),
        )
    }

suspend inline fun <T> Flow<OptionalLoadable<T>>.firstFinished(): T? =
    this
        .transform {
            when (it) {
                is OptionalLoadable.Failed -> throw it.cause
                OptionalLoadable.Loading -> {}
                is OptionalLoadable.LoadedAvailable -> emit(it.value)
                is OptionalLoadable.NotAvailable -> emit(null)
            }
        }.first()

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

fun <T> Flow<OptionalLoadable<T>>.stateIn(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(100),
): SharedFlow<OptionalLoadable<T>> =
    this
        .stateIn(
            scope,
            started,
            OptionalLoadable.Loading,
        )
