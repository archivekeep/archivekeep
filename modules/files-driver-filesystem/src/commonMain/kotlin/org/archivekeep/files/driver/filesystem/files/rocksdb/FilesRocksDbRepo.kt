package org.archivekeep.files.driver.filesystem.files.rocksdb

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.archivekeep.files.api.repository.PLAIN_REPOSITORY_TYPE
import org.archivekeep.files.driver.filesystem.files.FilesDatabaseRepo
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

private const val dbDirName = "rocksdb"

class FilesRocksDbRepo(
    root: Path,
    parentJob: Job? = null,
    archiveRoot: Path = root.resolve(".archive"),
    dbPath: Path = archiveRoot.resolve(dbDirName),
    stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FilesDatabaseRepo(root, parentJob, archiveRoot, stateDispatcher, ioDispatcher) {
    private val rocksDBStore = openRocksDBStore(dbPath.pathString, scope, ioDispatcher)
    override val indexStore: org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore =
        RocksDBIndexStore(rocksDBStore)

    init {
        startAutoRepair()
    }

    override fun closeSync() {
        rocksDBStore.close()
        super.closeSync()
    }

    override suspend fun close() {
        closeSync()
        job.join()
    }

    companion object {
        fun openOrNull(path: Path): FilesRocksDbRepo? {
            val archiveDir = path.resolve(".archive")
            val dbPath = archiveDir.resolve(dbDirName)

            if (dbPath.exists()) {
                return FilesRocksDbRepo(path)
            }

            return null
        }

        suspend fun create(
            path: Path,
            parentJob: Job? = null,
        ): FilesRocksDbRepo {
            val archiveDir = path.resolve(".archive")
            val dbPath = archiveDir.resolve(dbDirName)

            if (dbPath.isDirectory()) {
                throw RuntimeException("Already exists")
            }

            archiveDir.createDirectory()
            dbPath.createDirectory()

            return FilesRocksDbRepo(path, parentJob).apply {
                updateMetadata { it.copy(repositoryType = PLAIN_REPOSITORY_TYPE) }
            }
        }
    }
}
