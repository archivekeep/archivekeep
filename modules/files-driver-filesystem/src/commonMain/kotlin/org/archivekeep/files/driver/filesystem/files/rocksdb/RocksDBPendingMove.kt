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
            val model = rocksDbJson.decodeFromString(PendingMoveModel.serializer(), String(bytes, Charsets.UTF_8))
            val updated = model.copy(lastAlive = date.time)
            val json = rocksDbJson.encodeToString(PendingMoveModel.serializer(), updated)
            put(store.cfPendingMoves, to.encodeToByteArray(), json.encodeToByteArray())
        }
    }

    override suspend fun failed() {
        store.withDbAccess {
            delete(store.cfPendingMoves, to.encodeToByteArray())
        }
    }

    override suspend fun completed(lastModified: Date) {
        val newFileModel =
            FileModel(
                path = to,
                fileSize = size,
                checksumSha256 = checksumSha256,
                lastModified = lastModified.time,
            )
        val json = rocksDbJson.encodeToString(FileModel.serializer(), newFileModel)
        store.withDbAccess {
            delete(store.cfFiles, from.encodeToByteArray())
            delete(store.cfPendingMoves, to.encodeToByteArray())
            put(store.cfFiles, to.encodeToByteArray(), json.encodeToByteArray())
            store.flush()
        }
        store.notifyFilesChanged()
    }
}
