package org.archivekeep.utils.flows

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.archivekeep.utils.loading.Loadable

fun <T> (Flow<T>).logResourceLoad(name: String): Flow<T> = this.onEach { println("Resource loaded: $name") }

fun <T> (Flow<Loadable<T>>).logLoadableResourceLoad(name: String): Flow<Loadable<T>> =
    this.onEach {
        when (it) {
            is Loadable.Failed -> println("Resource failed to load: $name")
            is Loadable.Loaded -> println("Resource loaded: $name")
            Loadable.Loading -> {}
        }
    }
