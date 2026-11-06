package org.archivekeep.files.driver.filesystem.files

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.file.Path

abstract class AbstractFilesystemWorkingRepository(
    override val root: Path,
) : FilesystemWorkingRepository {
    init {
        synchronized(openRepositoriesLock) {
            if (openRepositories[root] != null) {
                throw IllegalStateException("Repository for $root is already open")
            }

            openRepositories[root] = this
            println("Filesystem repository of type ${javaClass.simpleName} is opening: $root")
        }
    }

    var closeJob: Job? = null

    final override fun closeSync() {
        println("${javaClass.simpleName}: closing $root")
        closeSyncInternal()
        closeJob =
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    suspendClose()
                } finally {
                    openRepositories.remove(root)
                    println("${javaClass.simpleName}: closed $root")
                }
            }
    }

    protected open fun closeSyncInternal() {}

    protected open suspend fun suspendClose() {}

    suspend fun waitForClose() {
        closeJob!!.join()
    }

    final override suspend fun close() {
        closeSync()
        waitForClose()
    }

    companion object {
        private val openRepositoriesLock = Any()
        private val openRepositories = mutableMapOf<Path, AbstractFilesystemWorkingRepository>()
    }
}
