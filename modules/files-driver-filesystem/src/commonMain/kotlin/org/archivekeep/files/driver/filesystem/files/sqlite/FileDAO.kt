package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDAO {
    @Query("SELECT * FROM FileEntity")
    fun getFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM FileEntity WHERE path = :path")
    suspend fun getFileByPath(path: String): FileEntity?

    @Insert
    suspend fun addFile(entry: FileEntity)

    @Query("DELETE FROM FileEntity WHERE path = :path")
    suspend fun removeFile(path: String)
}
