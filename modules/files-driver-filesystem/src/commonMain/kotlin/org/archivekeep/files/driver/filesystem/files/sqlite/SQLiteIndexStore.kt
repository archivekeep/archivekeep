package org.archivekeep.files.driver.filesystem.files.sqlite

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SQLiteIndexStore(
    val sqliteDataSource: SQLiteDataSource,
) {
    suspend fun getDeadIncomingFiles() = sqliteDataSource.getIncomingFilesAliveBefore(Date(Date().time - durationToDeath.inWholeMilliseconds))

    suspend inline fun incomingFile(
        path: String,
        tmpWritePath: String,
        size: Long,
        checksumSha256: String,
        crossinline block: suspend () -> Date,
    ) {
        sqliteDataSource.storeIncomingFile(path, tmpWritePath, size, checksumSha256, Date())

        val lastModified =
            try {
                coroutineScope {
                    launch {
                        sqliteDataSource.updateIncomingFileLastAlive(path, Date())
                        delay(aliveUpdateFrequency)
                    }

                    block()
                }
            } catch (e: Throwable) {
                withContext(NonCancellable) {
                    sqliteDataSource.removeIncomingFile(path)
                }

                throw e
            }

        sqliteDataSource.storeFile(
            path,
            size,
            lastModified,
            checksumSha256,
            dropIncomingFile = true,
        )
    }

    suspend fun getDeadPendingMoves() = sqliteDataSource.getMovesAliveBefore(Date(Date().time - durationToDeath.inWholeMilliseconds))

    suspend inline fun moveFile(
        from: String,
        to: String,
        crossinline block: suspend () -> Date,
    ) {
        val move = sqliteDataSource.beginMove(from, to, Date())

        val lastModified =
            try {
                coroutineScope {
                    launch {
                        move.updateMoveLastAlive(Date())
                        delay(aliveUpdateFrequency)
                    }

                    block()
                }
            } catch (e: Throwable) {
                withContext(NonCancellable) {
                    move.failed()
                }

                throw e
            }

        move.completed(lastModified)
    }
}
