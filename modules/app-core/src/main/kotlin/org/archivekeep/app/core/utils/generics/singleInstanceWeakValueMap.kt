package org.archivekeep.app.core.utils.generics

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache

fun <K : Any, V : Any> singleInstanceWeakValueMap(factory: (key: K) -> V): LoadingCache<K, V> =
    CacheBuilder
        .newBuilder()
        .weakValues()
        .build(
            object : CacheLoader<K, V>() {
                override fun load(key: K): V = factory(key)
            },
        )
