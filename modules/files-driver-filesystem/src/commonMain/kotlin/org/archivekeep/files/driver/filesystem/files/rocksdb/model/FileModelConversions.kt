package org.archivekeep.files.driver.filesystem.files.rocksdb.model

import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import java.util.*

internal fun FileModel.toDatabaseFileRecord(): DatabaseIndexStore.DatabaseFileRecord =
    DatabaseIndexStore.DatabaseFileRecord(
        path = this.path,
        size = this.fileSize,
        checksumSha256 = this.checksumSha256,
        lastModified = Date(this.lastModified),
    )
