package org.archivekeep.utils.collections

class ListItemChangeLogger<T>(
    val name: String,
) {
    var knownItems = setOf<T>()

    fun onNewItems(latestItems: Iterable<T>) {
        val latestItemsSet = latestItems.toSet()

        val newItems = latestItemsSet - knownItems
        val removedItems = knownItems - latestItemsSet

        if (newItems.isNotEmpty() || removedItems.isNotEmpty()) {
            println("$name:${newItems.toBulletList("new")}${removedItems.toBulletList("removed")}")
        } else {
            println("$name: no changes")
        }

        knownItems = latestItemsSet
    }
}

private fun <T> Iterable<T>.toBulletList(action: String): String = this.joinToString { "\n * $action: $it" }
