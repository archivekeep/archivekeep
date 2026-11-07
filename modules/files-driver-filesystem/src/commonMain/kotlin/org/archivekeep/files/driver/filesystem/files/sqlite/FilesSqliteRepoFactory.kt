package org.archivekeep.files.driver.filesystem.files.sqlite

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import org.archivekeep.files.driver.filesystem.files.FilesDatabaseRepoFactory
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.writeText

private const val dbName = "archivekeep.db"

val FilesSqliteRepoFactory =
    FilesDatabaseRepoFactory { path ->
        val archiveDir = path.resolve(".archive")
        val dbPath = archiveDir.resolve(dbName)

        object : FilesDatabaseRepoFactory.LocationCheck {
            override fun isRepo(): Boolean = dbPath.exists()

            override fun initializeNewRepo() {
                archiveDir.createDirectory()
                dbPath.writeText("")
            }

            override fun createStore(
                scope: CoroutineScope,
                stateDispatcher: CoroutineDispatcher,
                ioDispatcher: CoroutineDispatcher,
            ): DatabaseIndexStore {
                val database = createIndexDatabase(dbPath, ioDispatcher)
                val sqliteDataSource = SQLiteDataSource(database)
                return SQLiteIndexStore(sqliteDataSource)
            }
        }
    }
