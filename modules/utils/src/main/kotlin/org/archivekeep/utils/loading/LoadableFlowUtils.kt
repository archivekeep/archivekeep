package org.archivekeep.utils.loading

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform

fun <T> Flow<T>.mapToLoadable(): Flow<Loadable<T>> = this.mapToLoadable { it }

inline fun <T, R> Flow<T>.mapToLoadable(crossinline transform: suspend (value: T) -> R): Flow<Loadable<R>> = this.mapToLoadable("Map to loadable", transform)

inline fun <T, R> Flow<T>.mapToLoadable(
    message: String,
    crossinline transform: suspend (value: T) -> R,
): Flow<Loadable<R>> =
    this
        .map {
            Loadable.Loaded(transform(it)) as Loadable<R>
        }.catch {
            emit(
                Loadable.Failed(
                    RuntimeException(
                        "$message: ${it.message ?: it.toString()}",
                        it,
                    ),
                ),
            )
        }.onStart { emit(Loadable.Loading) }

fun <T, R> Loadable<T>.mapLoadedData(function: (data: T) -> R): Loadable<R> =
    when (this) {
        is Loadable.Loaded ->
            try {
                Loadable.Loaded(function(this.value))
            } catch (e: Throwable) {
                println("Failed to map: $e")
                e.printStackTrace()
                Loadable.Failed(e)
            }
        is Loadable.Loading ->
            this
        is Loadable.Failed ->
            this
    }

suspend fun <T, R> Loadable<T>.smapLoadedData(function: suspend (data: T) -> R): Loadable<R> =
    when (this) {
        is Loadable.Loaded ->
            try {
                Loadable.Loaded(function(this.value))
            } catch (e: Throwable) {
                println("Failed to map: $e")
                e.printStackTrace()
                Loadable.Failed(e)
            }
        is Loadable.Loading ->
            this
        is Loadable.Failed ->
            this
    }

fun <T, R> Flow<Loadable<T>>.mapLoadedData(function: suspend (data: T) -> R): Flow<Loadable<R>> = this.map { it.smapLoadedData(function) }

@ExperimentalCoroutinesApi
fun <T, R> Flow<Loadable<T>>.flatMapLatestLoadedData(function: (data: T) -> Flow<R>): Flow<Loadable<R>> =
    this.flatMapLatest {
        when (it) {
            is Loadable.Loaded ->
                try {
                    function(it.value).mapToLoadable()
                } catch (e: Throwable) {
                    flowOf(Loadable.Failed(e))
                }
            is Loadable.Loading ->
                flow { emit(it) }
            is Loadable.Failed ->
                flow { emit(it) }
        }
    }

@ExperimentalCoroutinesApi
fun <T, R> Flow<Loadable<T>>.flatMapLoadableFlow(function: (data: T) -> Flow<Loadable<R>>): Flow<Loadable<R>> =
    this.flatMapLatest {
        when (it) {
            is Loadable.Loaded ->
                function(it.value)
            is Loadable.Loading ->
                flow { emit(it) }
            is Loadable.Failed ->
                flow { emit(it) }
        }
    }

suspend fun <T> Flow<Loadable<T>>.firstLoadedOrFailure(): T =
    transform {
        when (it) {
            is Loadable.Loaded ->
                emit(it.value)
            is Loadable.Loading -> {}
            is Loadable.Failed ->
                throw RuntimeException("Wait first loaded failed: ${it.throwable}", it.throwable)
        }
    }.first()

fun <T> Flow<Loadable<T>>.waitLoadedValue(): Flow<T> = this.transform { if (it is Loadable.Loaded) emit(it.value) }
