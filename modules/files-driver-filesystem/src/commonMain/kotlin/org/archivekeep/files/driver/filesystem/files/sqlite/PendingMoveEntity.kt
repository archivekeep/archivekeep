package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class PendingMoveEntity(
    @PrimaryKey
    val to: String,
    val from: String,
    val size: Long,
    val checksumSha256: String,
    val lastAlive: Date,
)
