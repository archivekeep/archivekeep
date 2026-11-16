package org.archivekeep.files.api.repository

import kotlinx.coroutines.flow.Flow
import org.archivekeep.files.api.repository.operations.StatusOperation
import org.archivekeep.files.procedures.indexupdate.RepoPerformanceTracker
import org.archivekeep.utils.loading.Loadable
import java.nio.file.Path

interface LocalRepo : Repo {
    val localIndex: Flow<Loadable<StatusOperation.Result>>

    suspend fun contains(path: String): Boolean

    suspend fun findAllFiles(globs: List<String>): List<Path>

    suspend fun indexedFilenames(): List<String>

    suspend fun verifyFileExists(path: String): Boolean

    suspend fun fileChecksum(path: String): String

    suspend fun computeFileChecksum(path: Path): String

    suspend fun add(
        path: String,
        reindex: Boolean = false,
        performanceTracker: RepoPerformanceTracker = RepoPerformanceTracker.NoOp,
    ) {
        prepareAdd(path, performanceTracker).add(reindex = reindex)
    }

    suspend fun prepareAdd(
        path: String,
        performanceTracker: RepoPerformanceTracker = RepoPerformanceTracker.NoOp,
    ): PreparedAdd

    suspend fun remove(
        path: String,
        performanceTracker: RepoPerformanceTracker = RepoPerformanceTracker.NoOp,
    )

    fun getFileSize(filename: String): Long?

    interface PreparedAdd {
        val checksumSha256: String

        suspend fun add(reindex: Boolean)
    }
}
