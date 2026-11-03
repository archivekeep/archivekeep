package org.archivekeep.files.driver.filesystem.files.rocksdb

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import maryk.core.properties.IsPropertyContext
import maryk.core.properties.definitions.wrapper.IsValueDefinitionWrapper
import maryk.core.properties.references.IsPropertyReference
import maryk.core.query.changes.Change
import maryk.core.query.changes.change
import maryk.core.query.filters.Equals
import maryk.core.query.pairs.with
import maryk.core.query.requests.add
import maryk.core.query.requests.change
import maryk.core.query.requests.delete
import maryk.core.query.requests.get
import maryk.core.query.requests.scan
import maryk.core.query.responses.updates.AdditionUpdate
import maryk.core.query.responses.updates.ChangeUpdate
import maryk.core.query.responses.updates.InitialValuesUpdate
import maryk.core.query.responses.updates.IsUpdateResponse
import maryk.core.query.responses.updates.RemovalUpdate
import maryk.datastore.rocksdb.RocksDBDataStore
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import org.archivekeep.files.driver.filesystem.files.durationToDeath
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.FileModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.IncomingFileModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.PendingMoveModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.toDatabaseFileRecord
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.toIncomingFileRecord
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.toPendingMoveRecord
import java.util.*

class RocksDBIndexStore(
    private val store: RocksDBDataStore,
) : DatabaseIndexStore {
    override val files =
        callbackFlow {
            val records = mutableMapOf<String, DatabaseIndexStore.DatabaseFileRecord>()
            val updateFlow = store.executeFlow(FileModel.scan(limit = UInt.MAX_VALUE))
            updateFlow.collect { update: IsUpdateResponse<FileModel> ->
                when (update) {
                    is InitialValuesUpdate<FileModel> -> {
                        for ((key, values) in update.values) {
                            val keyStr = key.toString()
                            records[keyStr] = values.toDatabaseFileRecord()
                        }
                    }

                    is AdditionUpdate<FileModel> -> {
                        val keyStr = update.key.toString()
                        records[keyStr] = update.values.toDatabaseFileRecord()
                    }

                    is ChangeUpdate<FileModel> -> {
                        val keyStr = update.key.toString()
                        val resp = store.execute(FileModel.get(update.key))
                        val record =
                            resp.values
                                .firstOrNull()
                                ?.values
                                ?.toDatabaseFileRecord()
                        if (record != null) {
                            records[keyStr] = record
                        }
                    }

                    is RemovalUpdate<FileModel> -> {
                        records.remove(update.key.toString())
                    }
                }
                trySend(records.values.toList())
            }
            awaitClose()
        }

    override suspend fun file(path: String): DatabaseIndexStore.DatabaseFileRecord? {
        val response =
            store.execute(
                FileModel.scan(
                    where =
                        Equals(
                            (FileModel.path.ref() as IsPropertyReference<String, IsValueDefinitionWrapper<String, *, IsPropertyContext, *>, *>).with(path),
                        ),
                ),
            )
        return response.values
            .firstOrNull()
            ?.values
            ?.toDatabaseFileRecord()
    }

    override suspend fun storeFile(
        path: String,
        size: Long,
        lastModified: Date,
        checksumSha256: String,
        dropIncomingFile: Boolean,
        reindex: Boolean,
    ) {
        val fileValues =
            FileModel.create(
                FileModel.path with path,
                FileModel.fileSize with size,
                FileModel.checksumSha256 with checksumSha256,
                FileModel.lastModified with lastModified.time,
            )

        if (reindex) {
            val key = store.findFileKey(path)
            if (key != null) {
                store.execute(
                    FileModel.change(
                        key.change(
                            Change(
                                (FileModel.fileSize.ref() as IsPropertyReference<Long, IsValueDefinitionWrapper<Long, *, IsPropertyContext, *>, *>).with(size),
                            ),
                            Change(
                                (FileModel.checksumSha256.ref() as IsPropertyReference<String, IsValueDefinitionWrapper<String, *, IsPropertyContext, *>, *>)
                                    .with(
                                        checksumSha256,
                                    ),
                            ),
                            Change(
                                (FileModel.lastModified.ref() as IsPropertyReference<Long, IsValueDefinitionWrapper<Long, *, IsPropertyContext, *>, *>)
                                    .with(
                                        lastModified.time,
                                    ),
                            ),
                        ),
                    ),
                )
            } else {
                store.execute(FileModel.add(fileValues))
            }
        } else {
            store.execute(FileModel.add(fileValues))
        }

        if (dropIncomingFile) {
            removeIncomingFile(path)
        }
    }

    override suspend fun remove(path: String) {
        val key = store.findFileKey(path) ?: return
        store.execute(FileModel.delete(key))
    }

    override suspend fun storeIncomingFile(
        path: String,
        tmpWritePath: String,
        size: Long,
        checksumSha256: String,
        lastAlive: Date,
    ) {
        val values =
            IncomingFileModel.create(
                IncomingFileModel.path with path,
                IncomingFileModel.tmpWritePath with tmpWritePath,
                IncomingFileModel.fileSize with size,
                IncomingFileModel.checksumSha256 with checksumSha256,
                IncomingFileModel.lastAlive with lastAlive.time,
            )
        store.execute(IncomingFileModel.add(values))
    }

    override suspend fun updateIncomingFileLastAlive(
        path: String,
        date: Date,
    ) {
        val key = store.findIncomingFileKey(path) ?: return
        store.execute(
            IncomingFileModel.change(
                key.change(
                    Change(
                        (IncomingFileModel.lastAlive.ref() as IsPropertyReference<Long, IsValueDefinitionWrapper<Long, *, IsPropertyContext, *>, *>)
                            .with(
                                date.time,
                            ),
                    ),
                ),
            ),
        )
    }

    override suspend fun removeIncomingFile(path: String) {
        val key = store.findIncomingFileKey(path) ?: return
        store.execute(IncomingFileModel.delete(key))
    }

    override suspend fun getDeadIncomingFiles(): List<DatabaseIndexStore.IncomingFileRecord> {
        val cutoff = Date().time - durationToDeath.inWholeMilliseconds

        val response = store.execute(IncomingFileModel.scan(limit = UInt.MAX_VALUE))
        return response.values
            .mapNotNull { vw ->
                val values = vw.values
                val lastAlive = values { IncomingFileModel.lastAlive }
                if (lastAlive == null || lastAlive >= cutoff) null else vw
            }.map { vw -> vw.values.toIncomingFileRecord() }
    }

    override suspend fun getDeadPendingMoves(): List<DatabaseIndexStore.PendingMoveRecord> {
        val cutoff = Date().time - durationToDeath.inWholeMilliseconds

        val response = store.execute(PendingMoveModel.scan(limit = UInt.MAX_VALUE))
        return response.values
            .mapNotNull { vw ->
                val values = vw.values
                val lastAlive = values { PendingMoveModel.lastAlive }
                if (lastAlive == null || lastAlive >= cutoff) null else vw
            }.map { vw -> vw.values.toPendingMoveRecord() }
    }

    override suspend fun beginMove(
        from: String,
        to: String,
        lastAlive: Date,
    ): DatabaseIndexStore.PendingMove {
        val originalFile =
            file(from)
                ?: throw IllegalStateException("File not found: $from")

        val moveValues =
            PendingMoveModel.create(
                PendingMoveModel.to with to,
                PendingMoveModel.from with from,
                PendingMoveModel.fileSize with originalFile.size,
                PendingMoveModel.checksumSha256 with originalFile.checksumSha256,
                PendingMoveModel.lastAlive with lastAlive.time,
            )

        store.execute(PendingMoveModel.add(moveValues))

        return RocksDBPendingMove(
            store = store,
            from = from,
            to = to,
            size = originalFile.size,
            checksumSha256 = originalFile.checksumSha256,
        )
    }

    override suspend fun removePendingMove(to: String) {
        val key = store.findPendingMoveKey(to) ?: return
        store.execute(PendingMoveModel.delete(key))
    }
}
