package org.archivekeep.files.driver.filesystem.files.rocksdb.model

import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore

internal fun IncomingFileModel.toIncomingFileRecord(): DatabaseIndexStore.IncomingFileRecord =
    DatabaseIndexStore.IncomingFileRecord(
        path = this.path,
        tmpWritePath = this.tmpWritePath,
        size = this.fileSize,
        checksumSha256 = this.checksumSha256,
    )
