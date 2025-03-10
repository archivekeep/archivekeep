package org.archivekeep.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.shareIn

@DelicateCoroutinesApi
fun <T> Flow<T>.sharedResourceInGlobalScope(): SharedFlow<T> =
    this
        .conflate()
        .shareIn(
            GlobalScope,
            SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 100,
            ),
            replay = 1,
        )

fun <T> Flow<T>.shareResourceIn(
    scope: CoroutineScope,
    started: SharingStarted = SharingStarted.WhileSubscribed(100),
): SharedFlow<T> =
    this
        .conflate()
        .shareIn(
            scope,
            started,
            replay = 1,
        )
