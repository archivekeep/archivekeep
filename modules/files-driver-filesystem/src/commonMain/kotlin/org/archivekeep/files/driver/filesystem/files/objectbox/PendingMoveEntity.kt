package org.archivekeep.files.driver.filesystem.files.objectbox

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import java.util.Date

@Entity
data class PendingMoveEntity(
    @Id var id: Long = 0,
    @Index val to: String = "",
    val from: String = "",
    val size: Long = 0,
    val checksumSha256: String = "",
    val lastAlive: Date = Date(),
)
