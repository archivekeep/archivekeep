package org.archivekeep.app.core.utils.generics

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import org.archivekeep.utils.coroutines.shareResourceIn

class UniqueSharedFlowInstanceManager<T : Any, V>(
    val scope: CoroutineScope,
    val started: SharingStarted = SharingStarted.WhileSubscribed(100, 0),
    val factory: (key: T) -> Flow<V>,
) {
    private val values: LoadingCache<T, SharedFlow<V>> =
        CacheBuilder
            .newBuilder()
            .weakValues()
            .build(
                object : CacheLoader<T, SharedFlow<V>>() {
                    override fun load(key: T): SharedFlow<V> = factory(key).shareResourceIn(scope, started)
                },
            )

    operator fun get(key: T): SharedFlow<V> = values.get(key)
}
