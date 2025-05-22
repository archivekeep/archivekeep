package org.archivekeep.utils.flows

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transformLatest
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> (Flow<List<T>>).delayReductions(): Flow<List<T>> =
    flow {
        var nextEmit: Instant? = null
        var lastSize = 0

        this@delayReductions
            .transformLatest {
                if (nextEmit != null && lastSize > it.size) {
                    val neededDelay = java.time.Duration.between(Instant.now(), nextEmit)
                    if (neededDelay.toMillis() > 0) {
                        delay(neededDelay.toMillis())
                    }
                }

                emit(it)

                nextEmit = Instant.now().plusMillis(200)
                lastSize = it.size
            }.collect(this@flow)
    }
