package org.archivekeep.files.driver.filesystem.files.rocksdb.model

import kotlinx.serialization.Serializable
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore

@Serializable
data class IncomingFileModel(
    val path: String,
    val tmpWritePath: String,
    val fileSize: Long,
    val checksumSha256: String,
    val lastAlive: Long,
)

internal fun IncomingFileModel.toIncomingFileRecord(): DatabaseIndexStore.IncomingFileRecord =
    DatabaseIndexStore.IncomingFileRecord(
        path = this.path,
        tmpWritePath = this.tmpWritePath,
        size = this.fileSize,
        checksumSha256 = this.checksumSha256,
    )
