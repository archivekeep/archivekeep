package org.archivekeep.files.driver.filesystem.files.rocksdb

import maryk.datastore.rocksdb.RocksDBDataStore
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.FileModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.FileModelId
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.IncomingFileModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.IncomingFileModelId
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.PendingMoveModel
import org.archivekeep.files.driver.filesystem.files.rocksdb.model.PendingMoveModelId

internal fun openRocksDBStore(path: String): RocksDBDataStore =
    RocksDBDataStore(
        keepAllVersions = false,
        relativePath = path,
        dataModelsById =
            mapOf(
                FileModelId to FileModel,
                IncomingFileModelId to IncomingFileModel,
                PendingMoveModelId to PendingMoveModel,
            ),
    )
