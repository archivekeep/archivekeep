package org.archivekeep.files.driver.filesystem.files

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.archivekeep.files.api.repository.PLAIN_REPOSITORY_TYPE
import java.nio.file.Path

class FilesDatabaseRepoFactory(
    private val locationCheck: suspend (path: Path) -> LocationCheck,
) {
    interface LocationCheck {
        fun isRepo(): Boolean

        fun initializeNewRepo()

        fun createStore(
            scope: CoroutineScope,
            stateDispatcher: CoroutineDispatcher,
            ioDispatcher: CoroutineDispatcher,
        ): DatabaseIndexStore
    }

    suspend fun openOrNull(
        path: Path,
        stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): FilesDatabaseRepo? {
        val locationCheck = locationCheck(path)

        if (locationCheck.isRepo()) {
            return FilesDatabaseRepo.open(
                path,
                stateDispatcher = stateDispatcher,
                ioDispatcher = ioDispatcher,
            ) { scope, ioDispatcher ->
                locationCheck.createStore(scope, stateDispatcher, ioDispatcher)
            }
        }

        return null
    }

    suspend fun isRepo(path: Path) = openOrNull(path)?.also { it.close() } != null

    suspend fun create(
        path: Path,
        parentJob: Job? = null,
        stateDispatcher: CoroutineDispatcher = Dispatchers.Default,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): FilesDatabaseRepo {
        val locationCheck = locationCheck(path)

        if (locationCheck.isRepo()) {
            throw RuntimeException("Already exists")
        }

        locationCheck.initializeNewRepo()

        return FilesDatabaseRepo
            .open(path, parentJob, stateDispatcher, ioDispatcher) { scope, ioDispatcher ->
                locationCheck.createStore(scope, stateDispatcher, ioDispatcher)
            }.apply {
                updateMetadata { it.copy(repositoryType = PLAIN_REPOSITORY_TYPE) }
            }
    }
}
