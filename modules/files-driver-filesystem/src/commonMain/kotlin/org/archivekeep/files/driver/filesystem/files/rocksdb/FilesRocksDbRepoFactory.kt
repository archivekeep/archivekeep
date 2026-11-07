package org.archivekeep.files.driver.filesystem.files.rocksdb

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import org.archivekeep.files.driver.filesystem.files.FilesDatabaseRepoFactory
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.pathString

private const val dbDirName = "rocksdb"

val FilesRocksDbRepoFactory =
    FilesDatabaseRepoFactory { path ->
        val archiveDir = path.resolve(".archive")
        val dbPath = archiveDir.resolve(dbDirName)

        object : FilesDatabaseRepoFactory.LocationCheck {
            override fun isRepo(): Boolean = dbPath.exists()

            override fun initializeNewRepo() {
                archiveDir.createDirectory()
                dbPath.createDirectory()
            }

            override fun createStore(
                scope: CoroutineScope,
                stateDispatcher: CoroutineDispatcher,
                ioDispatcher: CoroutineDispatcher,
            ): DatabaseIndexStore {
                val rocksDBStore = openRocksDBStore(dbPath.pathString, scope, ioDispatcher)
                return RocksDBIndexStore(rocksDBStore)
            }
        }
    }
