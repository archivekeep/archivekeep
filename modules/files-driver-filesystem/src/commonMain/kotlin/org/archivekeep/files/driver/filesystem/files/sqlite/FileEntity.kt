package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class FileEntity(
    @PrimaryKey
    val path: String,
    val size: Long,
    val checksumSha256: String,
    val lastModified: Date,
)
