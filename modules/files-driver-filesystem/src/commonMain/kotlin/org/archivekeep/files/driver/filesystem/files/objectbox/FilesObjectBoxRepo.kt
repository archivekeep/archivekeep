package org.archivekeep.files.driver.filesystem.files.objectbox

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.archivekeep.files.api.repository.PLAIN_REPOSITORY_TYPE
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import org.archivekeep.files.driver.filesystem.files.FilesDatabaseRepo
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

private const val dbDirName = "objectbox"

class FilesObjectBoxRepo(
    root: Path,
    parentJob: Job? = null,
    archiveRoot: Path = root.resolve(".archive"),
    dbPath: Path = archiveRoot.resolve(dbDirName),
    stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FilesDatabaseRepo(root, parentJob, archiveRoot, stateDispatcher, ioDispatcher) {
    private val store = createBoxStore(dbPath)
    private val objectBoxDataSource = createObjectBoxDataSource(store)
    override val indexStore: DatabaseIndexStore = ObjectBoxIndexStore(objectBoxDataSource)

    init {
        startAutoRepair()
    }

    fun close() {
        cancelScope()
        store.close()
    }

    companion object {
        fun openOrNull(path: Path): FilesObjectBoxRepo? {
            val archiveDir = path.resolve(".archive")
            val dbPath = archiveDir.resolve(dbDirName)

            if (dbPath.exists()) {
                return FilesObjectBoxRepo(path)
            }

            return null
        }

        fun isRepo(path: Path) = openOrNull(path)?.also { it.close() } != null

        suspend fun create(
            path: Path,
            parentJob: Job? = null,
        ): FilesObjectBoxRepo {
            val archiveDir = path.resolve(".archive")
            val dbPath = archiveDir.resolve(dbDirName)

            if (dbPath.isDirectory()) {
                throw RuntimeException("Already exists")
            }

            archiveDir.createDirectory()
            dbPath.createDirectory()

            val repo = FilesObjectBoxRepo(path, parentJob)
            repo.updateMetadata { it.copy(repositoryType = PLAIN_REPOSITORY_TYPE) }
            return repo
        }
    }
}
