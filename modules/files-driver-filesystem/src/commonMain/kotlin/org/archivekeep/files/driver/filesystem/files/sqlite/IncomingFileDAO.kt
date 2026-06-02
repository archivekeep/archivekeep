package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface IncomingFileDAO {
    @Insert
    suspend fun addIncomingFile(incomingFile: IncomingFileEntity)

    @Query("DELETE FROM IncomingFileEntity WHERE path = :path")
    suspend fun removeIncomingFile(path: String)
}
