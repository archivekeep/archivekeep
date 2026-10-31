package org.archivekeep.files.driver.filesystem.files.objectbox

import io.objectbox.BoxStore
import kotlinx.coroutines.flow.Flow
import java.util.*

interface ObjectBoxPendingMove {
    val from: String
    val to: String
    val size: Long
    val checksumSha256: String

    suspend fun updateMoveLastAlive(date: Date)

    suspend fun failed()

    suspend fun completed(lastModified: Date)
}

interface ObjectBoxDataSource {
    val files: Flow<List<FileEntity>>

    suspend fun file(path: String): FileEntity?

    suspend fun storeFile(
        path: String,
        size: Long,
        lastModified: Date,
        checksumSha256: String,
        dropIncomingFile: Boolean,
        reindex: Boolean,
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

    suspend fun getIncomingFilesAliveBefore(date: Date): List<IncomingFileEntity>

    suspend fun getPendingMovesAliveBefore(date: Date): List<PendingMoveEntity>

    suspend fun beginMove(
        from: String,
        to: String,
        lastAlive: Date,
    ): ObjectBoxPendingMove

    suspend fun removePendingMove(to: String)
}

expect fun createObjectBoxDataSource(store: BoxStore): ObjectBoxDataSource
