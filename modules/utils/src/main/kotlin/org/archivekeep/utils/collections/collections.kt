package org.archivekeep.utils.collections

inline fun <T> List<T>.ifNotEmpty(action: (collection: List<T>) -> Unit) {
    if (this.isNotEmpty()) {
        action(this)
    }
}

fun <T> List<T>.limitSize(maxSize: Int) = this.subList(0, size.coerceAtMost(maxSize))
