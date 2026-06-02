package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.Room
import androidx.room.RoomDatabase
import java.nio.file.Path
import kotlin.io.path.absolutePathString

actual fun createDatabaseBuilder(path: Path): RoomDatabase.Builder<ArchiveDatabase> {
    return Room.databaseBuilder<ArchiveDatabase>(
        name = path.absolutePathString(),
    )
}
