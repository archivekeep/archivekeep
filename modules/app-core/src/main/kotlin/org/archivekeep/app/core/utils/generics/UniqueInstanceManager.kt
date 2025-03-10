package org.archivekeep.app.core.utils.generics

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache

class UniqueInstanceManager<T : Any, V : Any>(
    val factory: (key: T) -> V,
) {
    private val values: LoadingCache<T, V> =
        CacheBuilder
            .newBuilder()
            .weakValues()
            .build(
                object : CacheLoader<T, V>() {
                    override fun load(key: T): V = factory(key)
                },
            )

    operator fun get(key: T): V = values.get(key)
}
