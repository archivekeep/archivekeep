package org.archivekeep.utils.flows

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

fun <T> (Flow<T>).logResourceLoad(name: String): Flow<T> = this.onEach { println("Resource loaded: $name") }
