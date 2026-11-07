package org.archivekeep.files.driver.filesystem.files

import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * Common interface abstracting the storage layer for database-backed file repositories.
 */
interface DatabaseIndexStore {
    data class DatabaseFileRecord(
        val path: String,
        val size: Long,
        val checksumSha256: String,
        val lastModified: Date,
    )

    data class IncomingFileRecord(
        val path: String,
        val tmpWritePath: String,
        val size: Long,
        val checksumSha256: String,
    )

    data class PendingMoveRecord(
        val from: String,
        val to: String,
        val size: Long,
        val checksumSha256: String,
    )

    interface PendingMove {
        val from: String
        val to: String
        val size: Long
        val checksumSha256: String

        suspend fun updateMoveLastAlive(date: Date)

        suspend fun failed()

        suspend fun completed(lastModified: Date)
    }

    val files: Flow<List<DatabaseFileRecord>>

    suspend fun file(path: String): DatabaseFileRecord?

    suspend fun storeFile(
        path: String,
        size: Long,
        lastModified: Date,
        checksumSha256: String,
        dropIncomingFile: Boolean = false,
        reindex: Boolean = false,
    )

    suspend fun remove(path: String)

    suspend fun storeIncomingFile(
        path: String,
        tmpWritePath: String,
        size: Long,
        checksumSha256: String,
        lastAlive: Date,
    )

    suspend fun updateIncomingFileLastAlive(
        path: String,
        date: Date,
    )

    suspend fun removeIncomingFile(path: String)

    suspend fun getDeadIncomingFiles(): List<IncomingFileRecord>

    suspend fun getDeadPendingMoves(): List<PendingMoveRecord>

    suspend fun beginMove(
        from: String,
        to: String,
        lastAlive: Date,
    ): PendingMove

    suspend fun removePendingMove(to: String)

    fun closeSync()

    suspend fun waitForClose()
}
