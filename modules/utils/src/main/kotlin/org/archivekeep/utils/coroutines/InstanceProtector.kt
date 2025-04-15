package org.archivekeep.utils.coroutines

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class InstanceProtector<T> {
    private class Instance<T>(
        // keeps a strong reference to the instance
        @Suppress("unused")
        private val instance: T,
        key: CoroutineContext.Key<Instance<T>>,
    ) : AbstractCoroutineContextElement(key)

    fun forInstance(instance: T): CoroutineContext.Element = Instance(instance, key)

    private val key = object : CoroutineContext.Key<Instance<T>> {}
}
