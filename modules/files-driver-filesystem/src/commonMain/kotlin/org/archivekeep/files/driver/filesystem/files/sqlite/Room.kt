package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

expect fun createDatabaseBuilder(path: Path): RoomDatabase.Builder<ArchiveDatabase>

fun createIndexDatabase(
    path: Path,
    coroutineContext: CoroutineContext = Dispatchers.IO,
): ArchiveDatabase {
    return createDatabaseBuilder(path)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(coroutineContext)
        .build()
}
