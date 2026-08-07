package com.sirktv.app.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Single source of truth for favorite writes across all content types; each content DAO still owns its own read-side join for catalog browsing. */
@Dao
interface FavoriteDao {

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE contentId = :contentId AND contentType = :contentType)")
    suspend fun isFavorite(contentId: String, contentType: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun delete(contentId: String, contentType: String)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM favorites WHERE contentType = :contentType")
    suspend fun maxSortOrder(contentType: String): Int

    @Query("SELECT * FROM favorites WHERE contentType = :contentType ORDER BY sortOrder ASC")
    suspend fun listCustomOrder(contentType: String): List<FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE contentType = :contentType ORDER BY sortOrder ASC")
    fun observeCustomOrder(contentType: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE contentType = :contentType ORDER BY title COLLATE NOCASE ASC")
    fun observeAlphabetical(contentType: String): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE contentType = :contentType ORDER BY addedAtEpochMillis DESC")
    fun observeRecentlyAdded(contentType: String): Flow<List<FavoriteEntity>>

    @Query("UPDATE favorites SET sortOrder = :sortOrder WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun updateSortOrder(contentId: String, contentType: String, sortOrder: Int)
}
