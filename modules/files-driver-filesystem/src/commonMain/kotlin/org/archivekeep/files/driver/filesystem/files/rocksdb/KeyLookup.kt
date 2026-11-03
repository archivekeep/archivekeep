package org.archivekeep.files.driver.filesystem.files.rocksdb

import maryk.core.properties.IsPropertyContext
import maryk.core.properties.definitions.wrapper.IsValueDefinitionWrapper
import maryk.core.properties.references.IsPropertyReference
import maryk.core.properties.types.Key
import maryk.core.query.filters.Equals
import maryk.core.query.pairs.with
import maryk.core.query.requests.scan
import maryk.datastore.rocksdb.RocksDBDataStore
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.FileModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.IncomingFileModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.PendingMoveModel

internal suspend fun RocksDBDataStore.findFileKey(path: String): Key<FileModel>? {
    val response =
        execute(
            FileModel.scan(
                where = Equals((FileModel.path.ref() as IsPropertyReference<String, IsValueDefinitionWrapper<String, *, IsPropertyContext, *>, *>).with(path)),
            ),
        )
    return response.values.firstOrNull()?.key
}

internal suspend fun RocksDBDataStore.findIncomingFileKey(path: String): Key<IncomingFileModel>? {
    val response =
        execute(
            IncomingFileModel.scan(
                where =
                    Equals(
                        (IncomingFileModel.path.ref() as IsPropertyReference<String, IsValueDefinitionWrapper<String, *, IsPropertyContext, *>, *>).with(path),
                    ),
            ),
        )
    return response.values.firstOrNull()?.key
}

internal suspend fun RocksDBDataStore.findPendingMoveKey(to: String): Key<PendingMoveModel>? {
    val response =
        execute(
            PendingMoveModel.scan(
                where =
                    Equals(
                        (PendingMoveModel.to.ref() as IsPropertyReference<String, IsValueDefinitionWrapper<String, *, IsPropertyContext, *>, *>).with(to),
                    ),
            ),
        )
    return response.values.firstOrNull()?.key
}
