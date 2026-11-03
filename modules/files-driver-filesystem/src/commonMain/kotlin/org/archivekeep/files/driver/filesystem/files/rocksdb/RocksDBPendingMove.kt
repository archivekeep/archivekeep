package org.archivekeep.files.driver.filesystem.files.rocksdb

import maryk.core.properties.IsPropertyContext
import maryk.core.properties.definitions.wrapper.IsValueDefinitionWrapper
import maryk.core.properties.references.IsPropertyReference
import maryk.core.query.changes.Change
import maryk.core.query.changes.change
import maryk.core.query.pairs.with
import maryk.core.query.requests.add
import maryk.core.query.requests.change
import maryk.core.query.requests.delete
import maryk.datastore.rocksdb.RocksDBDataStore
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.FileModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.PendingMoveModel
import java.util.*

internal class RocksDBPendingMove(
    private val store: RocksDBDataStore,
    override val from: String,
    override val to: String,
    override val size: Long,
    override val checksumSha256: String,
) : DatabaseIndexStore.PendingMove {
    override suspend fun updateMoveLastAlive(date: Date) {
        val key = store.findPendingMoveKey(to) ?: return
        store.execute(
            PendingMoveModel.change(
                key.change(
                    Change(
                        (PendingMoveModel.lastAlive.ref() as IsPropertyReference<Long, IsValueDefinitionWrapper<Long, *, IsPropertyContext, *>, *>)
                            .with(
                                date.time,
                            ),
                    ),
                ),
            ),
        )
    }

    override suspend fun failed() {
        store.findPendingMoveKey(to)?.let { key ->
            store.execute(PendingMoveModel.delete(key))
        }
    }

    override suspend fun completed(lastModified: Date) {
        store.findFileKey(from)?.let { fileKey ->
            store.execute(FileModel.delete(fileKey))
        }

        store.findPendingMoveKey(to)?.let { moveKey ->
            store.execute(PendingMoveModel.delete(moveKey))
        }

        val newFileValues =
            FileModel.create(
                FileModel.path with to,
                FileModel.fileSize with size,
                FileModel.checksumSha256 with checksumSha256,
                FileModel.lastModified with lastModified.time,
            )
        store.execute(FileModel.add(newFileValues))
    }
}
