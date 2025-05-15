package org.archivekeep.utils.loading

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import org.archivekeep.utils.flows.logLoadableResourceLoad
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

inline fun <T, R> Flow<T>.produceLoadable(
    workDispatcher: CoroutineDispatcher,
    message: String,
    throttle: Duration? = 100.milliseconds,
    crossinline transform: suspend (value: T) -> R,
): Flow<Loadable<R>> {
    return this
        .mapToLoadable(message, transform)
        .transform {
            currentCoroutineContext().ensureActive()
            emit(it)

            throttle?.let { delay(it) }
        }
        .flowOn(workDispatcher)
}

inline fun <T, R> Flow<T>.produceLoadableStateIn(
    scope: CoroutineScope,
    workDispatcher: CoroutineDispatcher,
    message: String,
    throttle: Duration? = 100.milliseconds,
    crossinline transform: suspend (value: T) -> R,
): StateFlow<Loadable<R>> {
    return this
        .produceLoadable(
            workDispatcher,
            message,
            throttle,
            transform,
        )
        .logLoadableResourceLoad(message)
        .stateIn(scope)
}
