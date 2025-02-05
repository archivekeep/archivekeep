package org.archivekeep.app.core.utils.generics

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn

class UniqueSharedFlowInstanceManager<T, V>(
    val scope: CoroutineScope,
    val factory: (key: T) -> Flow<V>,
    val started: SharingStarted = SharingStarted.WhileSubscribed(100),
) {
    private val values: LoadingCache<T, SharedFlow<V>> =
        CacheBuilder
            .newBuilder()
            .weakValues()
            .build(
                object : CacheLoader<T, SharedFlow<V>>() {
                    override fun load(key: T): SharedFlow<V> = factory(key).shareIn(scope, started, 1)
                },
            )

    operator fun get(key: T): SharedFlow<V> = values.get(key)
}
