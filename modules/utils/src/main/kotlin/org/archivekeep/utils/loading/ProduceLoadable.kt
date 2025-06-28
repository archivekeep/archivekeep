package org.archivekeep.utils.loading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import org.archivekeep.utils.flows.logLoadableResourceLoad
import org.archivekeep.utils.io.watchForSingleFile
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

inline fun <T, R> Flow<T>.produceLoadable(
    workDispatcher: CoroutineDispatcher,
    message: String,
    throttle: Duration? = 100.milliseconds,
    crossinline transform: suspend (value: T) -> R,
): Flow<Loadable<R>> =
    this
        .mapToLoadable(message, transform)
        .transform {
            currentCoroutineContext().ensureActive()
            emit(it)

            throttle?.let { delay(it) }
        }.flowOn(workDispatcher)

inline fun <T, R> Flow<T>.produceLoadableStateIn(
    scope: CoroutineScope,
    workDispatcher: CoroutineDispatcher,
    message: String,
    throttle: Duration? = 100.milliseconds,
    crossinline transform: suspend (value: T) -> R,
): StateFlow<Loadable<R>> =
    this
        .produceLoadable(
            workDispatcher,
            message,
            throttle,
            transform,
        ).logLoadableResourceLoad(message)
        .stateIn(scope)

inline fun <R> Path.produceLoadableStateIn(
    scope: CoroutineScope,
    watchDispatcher: CoroutineDispatcher,
    workDispatcher: CoroutineDispatcher,
    message: String,
    throttle: Duration? = 100.milliseconds,
    crossinline transform: suspend (value: String) -> R,
): StateFlow<Loadable<R>> =
    this
        .watchForSingleFile(watchDispatcher)
        .map { "update" }
        .onStart { emit("start") }
        .conflate()
        .produceLoadableStateIn(
            scope = scope,
            workDispatcher = workDispatcher,
            message = message,
            throttle = throttle,
            transform = transform,
        )
