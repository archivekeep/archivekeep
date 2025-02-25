package org.archivekeep.app.core.utils.generics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import org.archivekeep.utils.loading.Loadable
import org.archivekeep.utils.loading.mapToLoadable

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

@DelicateCoroutinesApi
fun <T> Flow<T>.sharedGlobalLoadableWhileSubscribed(message: String? = null): SharedFlow<Loadable<T>> =
    this
        .mapToLoadable(message)
        .sharedGlobalWhileSubscribed()

fun <T> Flow<T>.sharedLoadableWhileSubscribed(scope: CoroutineScope): SharedFlow<Loadable<T>> =
    this
        .mapToLoadable()
        .sharedWhileSubscribed(scope)
