package org.archivekeep.files.driver.filesystem.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.archivekeep.files.api.repository.PLAIN_REPOSITORY_TYPE
import org.archivekeep.files.driver.filesystem.files.sqlite.SQLiteDataSource
import org.archivekeep.files.driver.filesystem.files.sqlite.SQLiteIndexStore
import org.archivekeep.files.driver.filesystem.files.sqlite.createIndexDatabase
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText

private const val dbName = "archivekeep.db"

class FilesSqliteRepo(
    root: Path,
    parentJob: Job? = null,
    archiveRoot: Path = root.resolve(".archive"),
    dbPath: Path = archiveRoot.resolve(dbName),
    stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FilesDatabaseRepo(root, parentJob, archiveRoot, stateDispatcher, ioDispatcher) {
    private val database = createIndexDatabase(dbPath, ioDispatcher)
    private val sqliteDataSource = SQLiteDataSource(database)
    override val indexStore: DatabaseIndexStore = SQLiteIndexStore(sqliteDataSource)

    init {
        startAutoRepair()
    }

    override fun closeSyncInternal() {
        database.close()
        super.closeSyncInternal()
    }

    companion object {
        fun openOrNull(path: Path): FilesSqliteRepo? {
            val archiveDir = path.resolve(".archive")
            val dbPath = archiveDir.resolve(dbName)

            if (dbPath.exists()) {
                return FilesSqliteRepo(path)
            }

            return null
        }

        suspend fun isRepo(path: Path) = openOrNull(path)?.also { it.close() } != null

        suspend fun create(
            path: Path,
            parentJob: Job? = null,
        ): FilesSqliteRepo {
            val archiveDir = path.resolve(".archive")
            val dbPath = archiveDir.resolve(dbName)

            if (dbPath.isDirectory()) {
                throw RuntimeException("Already exists")
            }

            archiveDir.createDirectory()
            dbPath.writeText("")

            return FilesSqliteRepo(path, parentJob).apply {
                updateMetadata { it.copy(repositoryType = PLAIN_REPOSITORY_TYPE) }
            }
        }
    }
}
