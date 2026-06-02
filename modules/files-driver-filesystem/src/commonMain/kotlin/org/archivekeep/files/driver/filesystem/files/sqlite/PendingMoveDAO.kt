package org.archivekeep.files.driver.filesystem.files.sqlite

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingMoveDAO {
    @Insert
    suspend fun addPendingMove(pendingMove: PendingMoveEntity)

    @Query("DELETE FROM PendingMoveEntity WHERE \"to\" = :to")
    suspend fun removePendingMove(to: String)
}
