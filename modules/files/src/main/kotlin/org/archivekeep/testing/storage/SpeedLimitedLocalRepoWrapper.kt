package org.archivekeep.testing.storage

import kotlinx.coroutines.delay
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.LocalRepo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import java.io.InputStream
import java.nio.file.Path
import kotlin.math.log10
import kotlin.math.nextUp

class SpeedLimitedLocalRepoWrapper(
    val base: LocalRepo,
) : LocalRepo {
    override suspend fun index(): RepoIndex =
        base.index().also {
            delay(lessThanLinearlyIncreasing(it.files.size))
        }

    override suspend fun move(
        from: String,
        to: String,
    ) = delayed(50) {
        base.move(from, to)
    }

    override suspend fun open(filename: String): Pair<ArchiveFileInfo, InputStream> =
        delayed(50) {
            base.open(filename)
        }

    override suspend fun save(
        filename: String,
        info: ArchiveFileInfo,
        stream: InputStream,
    ) = delayed(200) {
        base.save(filename, info, stream)
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

    override suspend fun add(path: String) =
        delayed(1000) {
            base.add(path)
        }

    override suspend fun remove(path: String) =
        delayed(100) {
            base.remove(path)
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

    override val observable
        get() = base.observable
}
