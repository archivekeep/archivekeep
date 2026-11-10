package org.archivekeep.files.driver.filesystem.files

/**
 * Operation IDs used to track performance of [DatabaseIndexStore] operations.
 */
object DatabaseIndexStoreOperationIds {
    const val STORE_FILE = "indexStore.storeFile"
    const val REMOVE = "indexStore.remove"
    const val BEGIN_MOVE = "indexStore.beginMove"
}
