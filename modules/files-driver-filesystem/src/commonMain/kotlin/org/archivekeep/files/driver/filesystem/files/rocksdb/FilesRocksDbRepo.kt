package org.archivekeep.files.driver.filesystem.files.rocksdb

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.archivekeep.files.api.repository.PLAIN_REPOSITORY_TYPE
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import org.archivekeep.files.driver.filesystem.files.FilesDatabaseRepo
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

private const val dbDirName = "rocksdb"

class FilesRocksDbRepo private constructor(
    root: Path,
    parentJob: Job? = null,
    archiveRoot: Path = root.resolve(".archive"),
    val dbPath: Path = archiveRoot.resolve(dbDirName),
    stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FilesDatabaseRepo(root, parentJob, archiveRoot, stateDispatcher, ioDispatcher) {
    private var rocksDBStore: RocksDBStore? = null
    override lateinit var indexStore: DatabaseIndexStore

    private fun init() {
        this.rocksDBStore = openRocksDBStore(dbPath.pathString, scope, ioDispatcher)
        this.indexStore = RocksDBIndexStore(rocksDBStore!!)

        startAutoRepair()
    }

    private suspend fun initOrCloseOnFailure() {
        try {
            init()
        } catch (e: Throwable) {
            // needs to be unregistered from AbstractFilesystemWorkingRepository.openRepositories
            this.close()
            throw e
        }
    }

    override fun closeSyncInternal() {
        rocksDBStore?.close()
        super.closeSyncInternal()
    }

    override suspend fun suspendClose() {
        rocksDBStore?.waitForClose()
    }

    companion object {
        suspend fun openOrNull(path: Path): FilesRocksDbRepo? {
            val archiveDir = path.resolve(".archive")
            val dbPath = archiveDir.resolve(dbDirName)

            if (dbPath.exists()) {
                return FilesRocksDbRepo(path).apply { initOrCloseOnFailure() }
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
                initOrCloseOnFailure()
                updateMetadata { it.copy(repositoryType = PLAIN_REPOSITORY_TYPE) }
            }
        }
    }
}
