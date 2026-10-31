package org.archivekeep.files.driver.filesystem.files.objectbox

import kotlinx.coroutines.flow.map
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import org.archivekeep.files.driver.filesystem.files.durationToDeath
import java.util.*

class ObjectBoxIndexStore(
    private val dataSource: ObjectBoxDataSource,
) : DatabaseIndexStore {
    override val files =
        dataSource.files.map { list ->
            list.map {
                DatabaseIndexStore.DatabaseFileRecord(
                    path = it.path,
                    size = it.size,
                    checksumSha256 = it.checksumSha256,
                    lastModified = it.lastModified,
                )
            }
        }

    override suspend fun file(path: String): DatabaseIndexStore.DatabaseFileRecord? =
        dataSource.file(path)?.let {
            DatabaseIndexStore.DatabaseFileRecord(
                path = it.path,
                size = it.size,
                checksumSha256 = it.checksumSha256,
                lastModified = it.lastModified,
            )
        }

    override suspend fun storeFile(
        path: String,
        size: Long,
        lastModified: Date,
        checksumSha256: String,
        dropIncomingFile: Boolean,
        reindex: Boolean,
    ) {
        dataSource.storeFile(path, size, lastModified, checksumSha256, dropIncomingFile, reindex)
    }

    override suspend fun remove(path: String) {
        dataSource.remove(path)
    }

    override suspend fun storeIncomingFile(
        path: String,
        tmpWritePath: String,
        size: Long,
        checksumSha256: String,
        lastAlive: Date,
    ) {
        dataSource.storeIncomingFile(path, tmpWritePath, size, checksumSha256, lastAlive)
    }

    override suspend fun updateIncomingFileLastAlive(
        path: String,
        date: Date,
    ) {
        dataSource.updateIncomingFileLastAlive(path, date)
    }

    override suspend fun removeIncomingFile(path: String) {
        dataSource.removeIncomingFile(path)
    }

    override suspend fun getDeadIncomingFiles(): List<DatabaseIndexStore.IncomingFileRecord> =
        dataSource.getIncomingFilesAliveBefore(Date(Date().time - durationToDeath.inWholeMilliseconds)).map {
            DatabaseIndexStore.IncomingFileRecord(
                path = it.path,
                tmpWritePath = it.tmpWritePath,
                size = it.size,
                checksumSha256 = it.checksumSha256,
            )
        }

    override suspend fun getDeadPendingMoves(): List<DatabaseIndexStore.PendingMoveRecord> =
        dataSource.getPendingMovesAliveBefore(Date(Date().time - durationToDeath.inWholeMilliseconds)).map {
            DatabaseIndexStore.PendingMoveRecord(
                from = it.from,
                to = it.to,
                size = it.size,
                checksumSha256 = it.checksumSha256,
            )
        }

    override suspend fun beginMove(
        from: String,
        to: String,
        lastAlive: Date,
    ): DatabaseIndexStore.PendingMove {
        val move = dataSource.beginMove(from, to, lastAlive)
        return object : DatabaseIndexStore.PendingMove {
            override val from: String = move.from
            override val to: String = move.to
            override val size: Long = move.size
            override val checksumSha256: String = move.checksumSha256

            override suspend fun updateMoveLastAlive(date: Date) {
                move.updateMoveLastAlive(date)
            }

            override suspend fun failed() {
                move.failed()
            }

            override suspend fun completed(lastModified: Date) {
                move.completed(lastModified)
            }
        }
    }

    override suspend fun removePendingMove(to: String) {
        dataSource.removePendingMove(to)
    }
}
