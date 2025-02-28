package org.archivekeep.app.core.utils.generics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

@DelicateCoroutinesApi
fun <T> Flow<T>.sharedGlobalWhileSubscribed(): SharedFlow<T> =
    this.shareIn(
        GlobalScope,
        SharingStarted.WhileSubscribed(
            stopTimeoutMillis = 1_000,
        ),
        replay = 1,
    )

fun <T> Flow<T>.sharedWhileSubscribed(scope: CoroutineScope): SharedFlow<T> =
    this.shareIn(
        scope,
        SharingStarted.WhileSubscribed(
            stopTimeoutMillis = 100,
        ),
        replay = 1,
    )
