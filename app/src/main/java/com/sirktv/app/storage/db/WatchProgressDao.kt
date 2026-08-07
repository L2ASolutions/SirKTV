package com.sirktv.app.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {

    @Query(
        """
        SELECT * FROM watch_progress
        WHERE contentType != 'LIVE' AND positionMs > 5000 AND positionMs < durationMs * 0.95
        ORDER BY updatedAtEpochMillis DESC LIMIT 30
        """
    )
    fun observeContinueWatching(): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress ORDER BY updatedAtEpochMillis DESC LIMIT 30")
    fun observeRecentlyWatched(): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE contentId = :contentId LIMIT 1")
    suspend fun get(contentId: String): WatchProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE contentId = :contentId")
    suspend fun remove(contentId: String)

    @Query("DELETE FROM watch_progress")
    suspend fun clearAll()
}
