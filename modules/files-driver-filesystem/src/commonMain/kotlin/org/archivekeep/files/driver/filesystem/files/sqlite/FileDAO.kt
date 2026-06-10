package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDAO {
    @Query("SELECT * FROM FileEntity")
    fun getFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM FileEntity WHERE path = :path")
    suspend fun getFileByPath(path: String): FileEntity?

    @Insert
    suspend fun addFile(file: FileEntity)

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("DELETE FROM FileEntity WHERE path = :path")
    suspend fun removeFile(path: String)
}
