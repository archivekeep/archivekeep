package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import java.util.Date

class SQLiteDataSource(
    private val database: ArchiveDatabase,
) {
    val files = database.fileDAO().getFiles()

    suspend fun file(path: String) = database.fileDAO().getFileByPath(path)

    suspend fun storeFile(
        path: String,
        size: Long,
        lastModified: Date,
        checksumSha256: String,
        dropIncomingFile: Boolean = false,
        reindex: Boolean = false,
    ) {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                val newFileEntity =
                    FileEntity(
                        path = path,
                        size = size,
                        lastModified = lastModified,
                        checksumSha256 = checksumSha256,
                    )

                if (!reindex) {
                    database.fileDAO().addFile(newFileEntity)
                } else {
                    database.fileDAO().updateFile(newFileEntity)
                }

                if (dropIncomingFile) {
                    database.incomingFileDAO().removeIncomingFile(path)
                }
            }
        }
    }

    suspend fun remove(path: String) {
        database.fileDAO().removeFile(path)
    }

    suspend fun storeIncomingFile(
        path: String,
        tmpWritePath: String,
        size: Long,
        checksumSha256: String,
    ) {
        database.incomingFileDAO().addIncomingFile(
            IncomingFileEntity(
                path = path,
                tmpWritePath = tmpWritePath,
                size = size,
                checksumSha256 = checksumSha256,
            ),
        )
    }

    suspend fun removeIncomingFile(path: String) {
        database.incomingFileDAO().removeIncomingFile(path)
    }

    suspend fun beginMove(
        from: String,
        to: String,
    ): PendingMove {
        val originalFile = database.fileDAO().getFileByPath(from)!!

        database.pendingMoveDAO().addPendingMove(
            PendingMoveEntity(
                from = from,
                to = to,
                checksumSha256 = originalFile.checksumSha256,
                size = originalFile.size,
            ),
        )

        return PendingMove(from, to, originalFile.size, originalFile.checksumSha256)
    }

    inner class PendingMove(
        val from: String,
        val to: String,
        val size: Long,
        val checksumSha256: String,
    ) {
        suspend fun completed(lastModified: Date) {
            database.useWriterConnection { transactor ->
                transactor.immediateTransaction {
                    database.fileDAO().removeFile(from)
                    database.pendingMoveDAO().removePendingMove(to)
                    database.fileDAO().addFile(
                        FileEntity(
                            path = to,
                            size = size,
                            checksumSha256 = checksumSha256,
                            lastModified = lastModified,
                        ),
                    )
                }
            }
        }
    }
}
