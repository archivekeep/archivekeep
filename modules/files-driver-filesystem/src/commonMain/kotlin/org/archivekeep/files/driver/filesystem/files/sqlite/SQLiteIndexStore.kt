package org.archivekeep.files.driver.filesystem.files.sqlite

import kotlinx.coroutines.flow.map
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import org.archivekeep.files.driver.filesystem.files.durationToDeath
import java.util.*

class SQLiteIndexStore(
    private val sqliteDataSource: SQLiteDataSource,
) : DatabaseIndexStore {
    override val files =
        sqliteDataSource.files.map { list ->
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
        sqliteDataSource.file(path)?.let {
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
        sqliteDataSource.storeFile(path, size, lastModified, checksumSha256, dropIncomingFile, reindex)
    }

    override suspend fun remove(path: String) {
        sqliteDataSource.remove(path)
    }

    override suspend fun storeIncomingFile(
        path: String,
        tmpWritePath: String,
        size: Long,
        checksumSha256: String,
        lastAlive: Date,
    ) {
        sqliteDataSource.storeIncomingFile(path, tmpWritePath, size, checksumSha256, lastAlive)
    }

    override suspend fun updateIncomingFileLastAlive(
        path: String,
        date: Date,
    ) {
        sqliteDataSource.updateIncomingFileLastAlive(path, date)
    }

    override suspend fun removeIncomingFile(path: String) {
        sqliteDataSource.removeIncomingFile(path)
    }

    override suspend fun getDeadIncomingFiles(): List<DatabaseIndexStore.IncomingFileRecord> =
        sqliteDataSource.getIncomingFilesAliveBefore(Date(Date().time - durationToDeath.inWholeMilliseconds)).map {
            DatabaseIndexStore.IncomingFileRecord(
                path = it.path,
                tmpWritePath = it.tmpWritePath,
                size = it.size,
                checksumSha256 = it.checksumSha256,
            )
        }

    override suspend fun getDeadPendingMoves(): List<DatabaseIndexStore.PendingMoveRecord> =
        sqliteDataSource.getMovesAliveBefore(Date(Date().time - durationToDeath.inWholeMilliseconds)).map {
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
        val move = sqliteDataSource.beginMove(from, to, lastAlive)
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
        sqliteDataSource.removePendingMove(to)
    }
}
