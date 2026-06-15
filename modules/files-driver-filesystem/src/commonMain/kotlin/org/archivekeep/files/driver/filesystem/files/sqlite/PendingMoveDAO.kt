package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.util.Date

@Dao
interface PendingMoveDAO {
    @Query("SELECT * FROM PendingMoveEntity WHERE lastAlive < :lastAlive")
    suspend fun getPendingMovesAliveBefore(lastAlive: Date): List<PendingMoveEntity>

    @Query("SELECT * FROM PendingMoveEntity WHERE \"to\" = :to")
    suspend fun getPendingMoveByPath(to: String): PendingMoveEntity?

    @Insert
    suspend fun addPendingMove(pendingMove: PendingMoveEntity)

    @Update
    suspend fun updatePendingMove(pendingMove: PendingMoveEntity)

    @Query("DELETE FROM PendingMoveEntity WHERE \"to\" = :to")
    suspend fun removePendingMove(to: String)
}
