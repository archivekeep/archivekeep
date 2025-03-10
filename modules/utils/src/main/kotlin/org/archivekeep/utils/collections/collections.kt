package org.archivekeep.utils.collections

inline fun <T> List<T>.ifNotEmpty(action: (collection: List<T>) -> Unit) {
    if (this.isNotEmpty()) {
        action(this)
    }
}
