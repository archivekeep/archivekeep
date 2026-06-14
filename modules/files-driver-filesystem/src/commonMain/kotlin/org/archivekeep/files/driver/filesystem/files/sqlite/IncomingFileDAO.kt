package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.util.Date

@Dao
interface IncomingFileDAO {
    @Query("SELECT * FROM IncomingFileEntity WHERE lastAlive < :lastAlive")
    suspend fun getIncomingFilesAliveBefore(lastAlive: Date): List<IncomingFileEntity>

    @Query("SELECT * FROM IncomingFileEntity WHERE path = :path")
    suspend fun getIncomingFileByPath(path: String): IncomingFileEntity?

    @Insert
    suspend fun addIncomingFile(incomingFile: IncomingFileEntity)

    @Update
    suspend fun updateIncomingFile(incomingFile: IncomingFileEntity)

    @Query("DELETE FROM IncomingFileEntity WHERE path = :path")
    suspend fun removeIncomingFile(path: String)
}
