package org.archivekeep.files.driver.filesystem.files.rocksdb.model

import kotlinx.serialization.Serializable
import org.archivekeep.files.driver.filesystem.files.DatabaseIndexStore

@Serializable
data class PendingMoveModel(
    val to: String,
    val from: String,
    val fileSize: Long,
    val checksumSha256: String,
    val lastAlive: Long,
)

internal fun PendingMoveModel.toPendingMoveRecord(): DatabaseIndexStore.PendingMoveRecord =
    DatabaseIndexStore.PendingMoveRecord(
        from = this.from,
        to = this.to,
        size = this.fileSize,
        checksumSha256 = this.checksumSha256,
    )
