package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import org.archivekeep.files.driver.filesystem.util.DateConverter

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ArchiveDatabaseConstructor : RoomDatabaseConstructor<ArchiveDatabase> {
    override fun initialize(): ArchiveDatabase
}

@Database(entities = [FileEntity::class, IncomingFileEntity::class, PendingMoveEntity::class], version = 1)
@TypeConverters(DateConverter::class)
@ConstructedBy(ArchiveDatabaseConstructor::class)
abstract class ArchiveDatabase : RoomDatabase() {
    abstract fun incomingFileDAO(): IncomingFileDAO

    abstract fun fileDAO(): FileDAO

    abstract fun pendingMoveDAO(): PendingMoveDAO
}
