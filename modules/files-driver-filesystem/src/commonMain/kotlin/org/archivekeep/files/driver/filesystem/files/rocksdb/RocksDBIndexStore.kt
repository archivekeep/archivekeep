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

private fun loadAllFiles(store: RocksDBStore): List<DatabaseIndexStore.DatabaseFileRecord> =
    store.withDbAccess {
        val records = mutableMapOf<String, DatabaseIndexStore.DatabaseFileRecord>()
        val iterator: RocksIterator = newIterator(store.cfFiles)
        try {
            iterator.seekToFirst()
            while (iterator.isValid()) {
                val key = String(iterator.key(), Charsets.UTF_8)
                val value = iterator.value()
                val fileModel = FileModel.parseFrom(value)
                records[key] = fileModel.toDatabaseFileRecord()
                iterator.next()
            }
        } finally {
            iterator.close()
        }
        records.values.toList()
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
                .debounce(RocksDBDefaults.filesChangedDebounce)
                .collect {
                    val updated = loadAllFiles(store)
                    trySend(updated)
                }

            awaitClose()
        }

    override suspend fun file(path: String): DatabaseIndexStore.DatabaseFileRecord? =
        store.withDbAccess {
            val bytes = get(store.cfFiles, path.encodeToByteArray())
            if (bytes == null) {
                null
            } else {
                val fileModel = FileModel.parseFrom(bytes)
                fileModel.toDatabaseFileRecord()
            }
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
            FileModel
                .newBuilder()
                .setPath(path)
                .setFileSize(size)
                .setChecksumSha256(checksumSha256)
                .setLastModified(lastModified.time)
                .build()
        store.withDbAccess {
            put(store.cfFiles, path.encodeToByteArray(), fileModel.toByteArray())
            store.flush()
        }
        store.notifyFilesChanged()

        if (dropIncomingFile) {
            removeIncomingFile(path)
        }
    }

    override suspend fun remove(path: String) {
        store.withDbAccess {
            delete(store.cfFiles, path.encodeToByteArray())
            store.flush()
        }
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
            IncomingFileModel
                .newBuilder()
                .setPath(path)
                .setTmpWritePath(tmpWritePath)
                .setFileSize(size)
                .setChecksumSha256(checksumSha256)
                .setLastAlive(lastAlive.time)
                .build()
        store.withDbAccess {
            put(store.cfIncomingFiles, path.encodeToByteArray(), model.toByteArray())
        }
    }

    override suspend fun updateIncomingFileLastAlive(
        path: String,
        date: Date,
    ) {
        store.withDbAccess {
            val bytes = get(store.cfIncomingFiles, path.encodeToByteArray()) ?: return@withDbAccess
            val model = IncomingFileModel.parseFrom(bytes)
            val updated = model.toBuilder().setLastAlive(date.time).build()
            put(store.cfIncomingFiles, path.encodeToByteArray(), updated.toByteArray())
        }
    }

    override suspend fun removeIncomingFile(path: String) {
        store.withDbAccess {
            delete(store.cfIncomingFiles, path.encodeToByteArray())
        }
    }

    override suspend fun getDeadIncomingFiles(): List<DatabaseIndexStore.IncomingFileRecord> {
        val cutoff = Date().time - durationToDeath.inWholeMilliseconds

        return store.withDbAccess {
            val iterator: RocksIterator = newIterator(store.cfIncomingFiles)
            try {
                iterator.seekToFirst()
                val result = mutableListOf<DatabaseIndexStore.IncomingFileRecord>()
                while (iterator.isValid()) {
                    val value = iterator.value()
                    val model = IncomingFileModel.parseFrom(value)
                    if (model.lastAlive < cutoff) {
                        result.add(model.toIncomingFileRecord())
                    }
                    iterator.next()
                }
                result
            } finally {
                iterator.close()
            }
        }
    }

    override suspend fun getDeadPendingMoves(): List<DatabaseIndexStore.PendingMoveRecord> {
        val cutoff = Date().time - durationToDeath.inWholeMilliseconds

        return store.withDbAccess {
            val iterator: RocksIterator = newIterator(store.cfPendingMoves)
            try {
                iterator.seekToFirst()
                val result = mutableListOf<DatabaseIndexStore.PendingMoveRecord>()
                while (iterator.isValid()) {
                    val value = iterator.value()
                    val model = PendingMoveModel.parseFrom(value)
                    if (model.lastAlive < cutoff) {
                        result.add(model.toPendingMoveRecord())
                    }
                    iterator.next()
                }
                result
            } finally {
                iterator.close()
            }
        }
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
            PendingMoveModel
                .newBuilder()
                .setTo(to)
                .setFrom(from)
                .setFileSize(originalFile.size)
                .setChecksumSha256(originalFile.checksumSha256)
                .setLastAlive(lastAlive.time)
                .build()
        store.withDbAccess {
            put(store.cfPendingMoves, to.encodeToByteArray(), moveModel.toByteArray())
        }

        return RocksDBPendingMove(
            store = store,
            from = from,
            to = to,
            size = originalFile.size,
            checksumSha256 = originalFile.checksumSha256,
        )
    }

    override suspend fun removePendingMove(to: String) {
        store.withDbAccess {
            delete(store.cfPendingMoves, to.encodeToByteArray())
        }
    }

    override fun closeSync() {
        store.close()
    }

    override suspend fun waitForClose() {
        store.waitForClose()
    }
}
