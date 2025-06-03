package org.archivekeep.testing.storage

import kotlinx.coroutines.delay
import org.archivekeep.files.repo.ArchiveFileInfo
import org.archivekeep.files.repo.Repo
import org.archivekeep.files.repo.RepoIndex
import org.archivekeep.files.repo.RepositoryMetadata
import java.io.InputStream
import kotlin.math.log10
import kotlin.math.nextUp

class SpeedLimitedRepoWrapper(
    val base: Repo,
) : Repo by base {
    override suspend fun index(): RepoIndex {
        val result = base.index()

        delay(logDescending(result.files.size))
        println("delay: ${logDescending(result.files.size)}")

        return result
    }

    override suspend fun move(
        from: String,
        to: String,
    ) = delayed(50) {
        base.move(from, to)
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
    ) = delayed(200) {
        base.save(filename, info, stream)
    }

    override suspend fun delete(filename: String) =
        delayed(25) {
            base.delete(filename)
        }

    override suspend fun getMetadata(): RepositoryMetadata =
        delayed(50) {
            base.getMetadata()
        }

    override suspend fun updateMetadata(transform: (old: RepositoryMetadata) -> RepositoryMetadata) =
        delayed(200) {
            base.updateMetadata(transform)
        }

    private suspend fun <T> delayed(
        duration: Long,
        work: suspend () -> T,
    ): T {
        val result = work()

        delay(duration)

        return result
    }

    private fun logDescending(v: Int): Long = v / log10((v * 1000).toDouble()).nextUp().toLong()
}
