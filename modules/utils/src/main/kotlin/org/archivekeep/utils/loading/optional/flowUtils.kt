package org.archivekeep.utils.loading.optional

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import org.archivekeep.utils.loading.Loadable

fun <T, R> Flow<OptionalLoadable<T>>.mapLoadedData(function: suspend (data: T) -> R): Flow<OptionalLoadable<R>> =
    this
        .map {
            when (it) {
                is OptionalLoadable.LoadedAvailable ->
                    OptionalLoadable.LoadedAvailable(function(it.value))

                is OptionalLoadable.Failed -> OptionalLoadable.Failed(it.cause)
                OptionalLoadable.Loading -> OptionalLoadable.Loading
                is OptionalLoadable.NotAvailable -> it
            }
        }.autoCatch()

fun <T, R> Flow<OptionalLoadable<T>>.mapLoaded(transform: suspend (data: T) -> OptionalLoadable<R>): Flow<OptionalLoadable<R>> =
    this
        .map {
            when (it) {
                is OptionalLoadable.LoadedAvailable -> transform(it.value)
                is OptionalLoadable.Failed -> it
                OptionalLoadable.Loading -> OptionalLoadable.Loading
                is OptionalLoadable.NotAvailable -> it
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

fun <T> Flow<OptionalLoadable<T>>.stateIn(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(100, 0),
): StateFlow<OptionalLoadable<T>> =
    this
        .stateIn(
            scope,
            started,
            OptionalLoadable.Loading,
        )

suspend inline fun <reified T> Flow<OptionalLoadable<T>>.firstFinishedLoading(): OptionalLoadable.LoadingFinished<T> =
    this.filterIsInstance<OptionalLoadable.LoadingFinished<T>>().first()
