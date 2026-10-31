package org.archivekeep.files.driver.filesystem.files.objectbox

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import java.util.Date

@Entity
data class IncomingFileEntity(
    @Id var id: Long = 0,
    @Index val path: String = "",
    val tmpWritePath: String = "",
    val size: Long = 0,
    val checksumSha256: String = "",
    val lastAlive: Date = Date(),
)
