package org.archivekeep.files.driver.filesystem.files.rocksdb

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import maryk.rocksdb.RocksIterator
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import org.archivekeep.files.driver.filesystem.files.durationToDeath
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.FileModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.IncomingFileModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.PendingMoveModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.toDatabaseFileRecord
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.toIncomingFileRecord
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.toPendingMoveRecord
import java.util.*
import kotlin.time.Duration.Companion.seconds

private val tag = "RocksDBIndexStore"

private fun loadAllFiles(store: RocksDBStore): List<DatabaseIndexStore.DatabaseFileRecord> {
    val records = mutableMapOf<String, DatabaseIndexStore.DatabaseFileRecord>()
    val iterator: RocksIterator = store.db.newIterator(store.cfFiles)
    iterator.seekToFirst()
    while (iterator.isValid()) {
        val key = String(iterator.key(), Charsets.UTF_8)
        val value = iterator.value()
        val fileModel = rocksDbJson.decodeFromString(FileModel.serializer(), String(value, Charsets.UTF_8))
        records[key] = fileModel.toDatabaseFileRecord()
        iterator.next()
    }
    iterator.close()
    return records.values.toList()
}

class RocksDBIndexStore(
    private val store: RocksDBStore,
) : DatabaseIndexStore {
    @OptIn(FlowPreview::class)
    override val files =
        callbackFlow {
            val initial = loadAllFiles(store)
            trySend(initial)

            store
                .filesChanged
                .debounce(3.seconds)
                .collect {
                    val updated = loadAllFiles(store)
                    trySend(updated)
                }

            awaitClose()
        }

    override suspend fun file(path: String): DatabaseIndexStore.DatabaseFileRecord? {
        val bytes = store.db.get(store.cfFiles, path.encodeToByteArray())
        val result =
            if (bytes == null) {
                null
            } else {
                val fileModel = rocksDbJson.decodeFromString(FileModel.serializer(), String(bytes, Charsets.UTF_8))
                fileModel.toDatabaseFileRecord()
            }
        return result
    }

    override suspend fun storeFile(
        path: String,
        size: Long,
        lastModified: Date,
        checksumSha256: String,
        dropIncomingFile: Boolean,
        reindex: Boolean,
    ) {
        val fileModel =
            FileModel(
                path = path,
                fileSize = size,
                checksumSha256 = checksumSha256,
                lastModified = lastModified.time,
            )
        val json = rocksDbJson.encodeToString(FileModel.serializer(), fileModel)
        store.db.put(store.cfFiles, path.encodeToByteArray(), json.encodeToByteArray())
        store.flush()
        store.notifyFilesChanged()

        if (dropIncomingFile) {
            removeIncomingFile(path)
        }
    }

    override suspend fun remove(path: String) {
        store.db.delete(store.cfFiles, path.encodeToByteArray())
        store.flush()
        store.notifyFilesChanged()
    }

    override suspend fun storeIncomingFile(
        path: String,
        tmpWritePath: String,
        size: Long,
        checksumSha256: String,
        lastAlive: Date,
    ) {
        val model =
            IncomingFileModel(
                path = path,
                tmpWritePath = tmpWritePath,
                fileSize = size,
                checksumSha256 = checksumSha256,
                lastAlive = lastAlive.time,
            )
        val json = rocksDbJson.encodeToString(IncomingFileModel.serializer(), model)
        store.db.put(store.cfIncomingFiles, path.encodeToByteArray(), json.encodeToByteArray())
    }

    override suspend fun updateIncomingFileLastAlive(
        path: String,
        date: Date,
    ) {
        val bytes = store.db.get(store.cfIncomingFiles, path.encodeToByteArray())
        if (bytes == null) return
        val model = rocksDbJson.decodeFromString(IncomingFileModel.serializer(), String(bytes, Charsets.UTF_8))
        val updated = model.copy(lastAlive = date.time)
        val json = rocksDbJson.encodeToString(IncomingFileModel.serializer(), updated)
        store.db.put(store.cfIncomingFiles, path.encodeToByteArray(), json.encodeToByteArray())
    }

    override suspend fun removeIncomingFile(path: String) {
        store.db.delete(store.cfIncomingFiles, path.encodeToByteArray())
    }

    override suspend fun getDeadIncomingFiles(): List<DatabaseIndexStore.IncomingFileRecord> {
        val cutoff = Date().time - durationToDeath.inWholeMilliseconds

        val iterator: RocksIterator = store.db.newIterator(store.cfIncomingFiles)
        iterator.seekToFirst()
        val result = mutableListOf<DatabaseIndexStore.IncomingFileRecord>()
        while (iterator.isValid()) {
            val value = iterator.value()
            val model = rocksDbJson.decodeFromString(IncomingFileModel.serializer(), String(value, Charsets.UTF_8))
            if (model.lastAlive < cutoff) {
                result.add(model.toIncomingFileRecord())
            }
            iterator.next()
        }
        iterator.close()
        return result
    }

    override suspend fun getDeadPendingMoves(): List<DatabaseIndexStore.PendingMoveRecord> {
        val cutoff = Date().time - durationToDeath.inWholeMilliseconds

        val iterator: RocksIterator = store.db.newIterator(store.cfPendingMoves)
        iterator.seekToFirst()
        val result = mutableListOf<DatabaseIndexStore.PendingMoveRecord>()
        while (iterator.isValid()) {
            val value = iterator.value()
            val model = rocksDbJson.decodeFromString(PendingMoveModel.serializer(), String(value, Charsets.UTF_8))
            if (model.lastAlive < cutoff) {
                result.add(model.toPendingMoveRecord())
            }
            iterator.next()
        }
        iterator.close()
        return result
    }

    override suspend fun beginMove(
        from: String,
        to: String,
        lastAlive: Date,
    ): DatabaseIndexStore.PendingMove {
        val originalFile =
            file(from)
                ?: throw IllegalStateException("File not found: $from")

        val moveModel =
            PendingMoveModel(
                to = to,
                from = from,
                fileSize = originalFile.size,
                checksumSha256 = originalFile.checksumSha256,
                lastAlive = lastAlive.time,
            )
        val json = rocksDbJson.encodeToString(PendingMoveModel.serializer(), moveModel)
        store.db.put(store.cfPendingMoves, to.encodeToByteArray(), json.encodeToByteArray())

        return RocksDBPendingMove(
            store = store,
            from = from,
            to = to,
            size = originalFile.size,
            checksumSha256 = originalFile.checksumSha256,
        )
    }

    override suspend fun removePendingMove(to: String) {
        store.db.delete(store.cfPendingMoves, to.encodeToByteArray())
    }
}
