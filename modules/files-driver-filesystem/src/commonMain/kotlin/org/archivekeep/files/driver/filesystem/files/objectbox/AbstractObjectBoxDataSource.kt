package org.archivekeep.files.driver.filesystem.files.objectbox

import io.objectbox.Box
import io.objectbox.kotlin.toFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

abstract class AbstractObjectBoxDataSource(
    protected val store: io.objectbox.BoxStore,
) : ObjectBoxDataSource {
    protected abstract val fileBox: Box<FileEntity>
    protected abstract val incomingFileBox: Box<IncomingFileEntity>
    protected abstract val pendingMoveBox: Box<PendingMoveEntity>

    // --- Abstract query methods that use generated Entity_ classes ---

    protected abstract fun queryFileByPath(path: String): FileEntity?

    protected abstract fun queryIncomingFileByPath(path: String): IncomingFileEntity?

    protected abstract fun queryPendingMoveByPath(to: String): PendingMoveEntity?

    protected abstract fun queryIncomingFilesAliveBefore(date: Date): List<IncomingFileEntity>

    protected abstract fun queryPendingMovesAliveBefore(date: Date): List<PendingMoveEntity>

    // --- Interface implementations ---

    override val files: Flow<List<FileEntity>>
        get() {
            return fileBox
                .query()
                .build()
                .subscribe()
                .toFlow()
                .map { it.toList() }
        }

    override suspend fun file(path: String): FileEntity? = queryFileByPath(path)

    override suspend fun storeFile(
        path: String,
        size: Long,
        lastModified: Date,
        checksumSha256: String,
        dropIncomingFile: Boolean,
        reindex: Boolean,
    ) {
        val newFileEntity =
            FileEntity(
                path = path,
                size = size,
                lastModified = lastModified,
                checksumSha256 = checksumSha256,
            )

        if (!reindex) {
            fileBox.put(newFileEntity)
        } else {
            val existing = file(path)
            if (existing != null) {
                fileBox.put(
                    newFileEntity.copy(id = existing.id),
                )
            } else {
                fileBox.put(newFileEntity)
            }
        }

        if (dropIncomingFile) {
            removeIncomingFile(path)
        }
    }

    override suspend fun remove(path: String) {
        val entity = file(path)
        if (entity != null) {
            fileBox.remove(entity.id)
        }
    }

    override suspend fun storeIncomingFile(
        path: String,
        tmpWritePath: String,
        size: Long,
        checksumSha256: String,
        lastAlive: Date,
    ) {
        incomingFileBox.put(
            IncomingFileEntity(
                path = path,
                tmpWritePath = tmpWritePath,
                size = size,
                checksumSha256 = checksumSha256,
                lastAlive = lastAlive,
            ),
        )
    }

    override suspend fun updateIncomingFileLastAlive(
        path: String,
        date: Date,
    ) {
        val entity = queryIncomingFileByPath(path)!!
        incomingFileBox.put(entity.copy(lastAlive = date))
    }

    override suspend fun removeIncomingFile(path: String) {
        val entity = queryIncomingFileByPath(path)
        if (entity != null) {
            incomingFileBox.remove(entity.id)
        }
    }

    override suspend fun getIncomingFilesAliveBefore(date: Date): List<IncomingFileEntity> = queryIncomingFilesAliveBefore(date)

    override suspend fun getPendingMovesAliveBefore(date: Date): List<PendingMoveEntity> = queryPendingMovesAliveBefore(date)

    override suspend fun beginMove(
        from: String,
        to: String,
        lastAlive: Date,
    ): ObjectBoxPendingMove {
        val originalFile = file(from)!!

        pendingMoveBox.put(
            PendingMoveEntity(
                from = from,
                to = to,
                checksumSha256 = originalFile.checksumSha256,
                size = originalFile.size,
                lastAlive = lastAlive,
            ),
        )

        return ObjectBoxPendingMoveImpl(from, to, originalFile.size, originalFile.checksumSha256)
    }

    override suspend fun removePendingMove(to: String) {
        val entity = queryPendingMoveByPath(to)
        if (entity != null) {
            pendingMoveBox.remove(entity.id)
        }
    }

    open inner class ObjectBoxPendingMoveImpl(
        override val from: String,
        override val to: String,
        override val size: Long,
        override val checksumSha256: String,
    ) : ObjectBoxPendingMove {
        override suspend fun updateMoveLastAlive(date: Date) {
            val entity = queryPendingMoveByPath(to)!!
            pendingMoveBox.put(entity.copy(lastAlive = date))
        }

        override suspend fun failed() {
            removePendingMove(to)
        }

        override suspend fun completed(lastModified: Date) {
            val originalFile = file(from)
            if (originalFile != null) {
                fileBox.remove(originalFile.id)
            }
            removePendingMove(to)
            fileBox.put(
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
