package org.archivekeep.files.driver.filesystem.files.rocksdb

import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.FileModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.PendingMoveModel
import java.util.*

internal class RocksDBPendingMove(
    private val store: RocksDBStore,
    override val from: String,
    override val to: String,
    override val size: Long,
    override val checksumSha256: String,
) : DatabaseIndexStore.PendingMove {
    override suspend fun updateMoveLastAlive(date: Date) {
        store.withDbAccess {
            val bytes = get(store.cfPendingMoves, to.encodeToByteArray()) ?: return@withDbAccess
            val model = PendingMoveModel.parseFrom(bytes)
            val updated = model.toBuilder().setLastAlive(date.time).build()
            put(store.cfPendingMoves, to.encodeToByteArray(), updated.toByteArray())
        }
    }

    override suspend fun failed() {
        store.withDbAccess {
            delete(store.cfPendingMoves, to.encodeToByteArray())
        }
    }

    override suspend fun completed(lastModified: Date) {
        val newFileModel =
            FileModel
                .newBuilder()
                .setPath(to)
                .setFileSize(size)
                .setChecksumSha256(checksumSha256)
                .setLastModified(lastModified.time)
                .build()
        store.withDbAccess {
            delete(store.cfFiles, from.encodeToByteArray())
            delete(store.cfPendingMoves, to.encodeToByteArray())
            put(store.cfFiles, to.encodeToByteArray(), newFileModel.toByteArray())
            store.flush()
        }
        store.notifyFilesChanged()
    }
}
