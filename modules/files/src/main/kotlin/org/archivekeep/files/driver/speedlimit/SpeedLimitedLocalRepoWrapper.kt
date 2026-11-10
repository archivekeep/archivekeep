package org.archivekeep.files.driver.speedlimit

import kotlinx.coroutines.delay
import org.archivekeep.files.api.repository.ArchiveFileInfo
import org.archivekeep.files.api.repository.LocalRepo
import org.archivekeep.files.api.repository.RepoIndex
import org.archivekeep.files.api.repository.RepositoryMetadata
import org.archivekeep.files.procedures.indexupdate.RepoPerformanceTracker
import java.io.InputStream
import java.nio.file.Path
import kotlin.math.log10
import kotlin.math.nextUp

class SpeedLimitedLocalRepoWrapper(
    val base: LocalRepo,
) : LocalRepo by base {
    override suspend fun index(): RepoIndex =
        base.index().also {
            delay(lessThanLinearlyIncreasing(it.files.size))
        }

    override suspend fun move(
        from: String,
        to: String,
        performanceTracker: RepoPerformanceTracker,
    ) = delayed(50) {
        base.move(from, to, performanceTracker)
    }

    override suspend fun <T> open(
        filename: String,
        block: suspend (ArchiveFileInfo, InputStream) -> T,
    ): T =
        delayed(50) {
            base.open(filename, block)
        }

    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
        monitor: (copiedBytes: Long) -> Unit,
        performanceTracker: RepoPerformanceTracker,
    ) = delayed(200) {
        base.save(filename, info, stream, monitor, performanceTracker)
    }

    override suspend fun delete(filename: String) =
        delayed(25) {
            base.delete(filename)
        }

    override suspend fun getMetadata(): RepositoryMetadata =
        delayed(25) {
            base.getMetadata()
        }

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) =
        delayed(200) {
            base.updateMetadata(transform)
        }

    override suspend fun contains(path: String): Boolean =
        delayed(1) {
            base.contains(path)
        }

    override suspend fun findAllFiles(globs: List<String>): List<Path> =
        base.findAllFiles(globs).also {
            delay(lessThanLinearlyIncreasing(it.size))
        }

    override suspend fun indexedFilenames(): List<String> =
        base.indexedFilenames().also {
            delay(lessThanLinearlyIncreasing(it.size))
        }

    override suspend fun verifyFileExists(path: String): Boolean =
        delayed(1) {
            base.verifyFileExists(path)
        }

    override suspend fun fileChecksum(path: String): String =
        delayed(5) {
            base.fileChecksum(path)
        }

    override suspend fun computeFileChecksum(path: Path): String =
        delayed(500) {
            base.computeFileChecksum(path)
        }

    override suspend fun add(
        path: String,
        reindex: Boolean,
        performanceTracker: RepoPerformanceTracker,
    ) = delayed(1000) {
        base.add(path, reindex, performanceTracker)
    }

    override suspend fun remove(
        path: String,
        performanceTracker: RepoPerformanceTracker,
    ) = delayed(100) {
        base.remove(path, performanceTracker)
    }

    private suspend fun <T> delayed(
        duration: Long,
        work: suspend () -> T,
    ): T {
        val result = work()

        delay(duration)

        return result
    }

    private fun lessThanLinearlyIncreasing(v: Int): Long = v / log10((v * 1000).toDouble()).nextUp().toLong()
}
