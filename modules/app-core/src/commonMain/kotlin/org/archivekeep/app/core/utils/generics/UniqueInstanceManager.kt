package org.archivekeep.app.core.utils.generics

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache

class UniqueInstanceManager<T : Any, V : Any>(
    val closeCallback: suspend (V) -> Unit = {},
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

    fun get(keys: Collection<T>): List<V> = keys.map { get(it) }

    suspend fun invalidate(key: T) {
        values.getIfPresent(key)?.let { old ->
            closeCallback(old)
        }
        values.invalidate(key)
    }
}
