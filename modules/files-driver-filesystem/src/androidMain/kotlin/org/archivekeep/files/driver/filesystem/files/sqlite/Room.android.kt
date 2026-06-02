package org.archivekeep.files.driver.filesystem.files.sqlite

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private lateinit var applicationContext: Context

fun initDatabase(appContext: Context) {
    // prevent double initialization
    if (::applicationContext.isInitialized) return
    applicationContext = appContext
}

actual fun createDatabaseBuilder(path: Path): RoomDatabase.Builder<ArchiveDatabase> {
    return Room.databaseBuilder<ArchiveDatabase>(
        context = applicationContext,
        name = path.absolutePathString(),
    )
}
