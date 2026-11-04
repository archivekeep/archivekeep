package org.archivekeep.files.driver.filesystem.files.rocksdb.model

import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore

internal fun PendingMoveModel.toPendingMoveRecord(): DatabaseIndexStore.PendingMoveRecord =
    DatabaseIndexStore.PendingMoveRecord(
        from = this.from,
        to = this.to,
        size = this.fileSize,
        checksumSha256 = this.checksumSha256,
    )
