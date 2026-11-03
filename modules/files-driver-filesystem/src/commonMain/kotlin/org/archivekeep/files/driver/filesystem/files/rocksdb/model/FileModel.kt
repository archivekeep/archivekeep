package org.archivekeep.files.driver.filesystem.files.rocksdb.model

import kotlinx.serialization.Serializable
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore
import java.util.*

@Serializable
data class FileModel(
    val path: String,
    val fileSize: Long,
    val checksumSha256: String,
    val lastModified: Long,
)

internal fun FileModel.toDatabaseFileRecord(): DatabaseIndexStore.DatabaseFileRecord =
    DatabaseIndexStore.DatabaseFileRecord(
        path = this.path,
        size = this.fileSize,
        checksumSha256 = this.checksumSha256,
        lastModified = Date(this.lastModified),
    )
