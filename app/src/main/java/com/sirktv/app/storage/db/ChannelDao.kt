package com.sirktv.app.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Non-entity projection used for reads — joins favorite membership onto a channel row. */
data class ChannelWithFavorite(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val categoryId: String,
    val channelNumber: Int,
    val isFavorite: Boolean
)

@Dao
interface ChannelDao {

    @Query("SELECT * FROM categories WHERE contentType = 'LIVE' ORDER BY name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT channels.id AS id, channels.name AS name, channels.logoUrl AS logoUrl,
               channels.categoryId AS categoryId, channels.channelNumber AS channelNumber,
               CASE WHEN favorites.contentId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM channels
        LEFT JOIN favorites ON channels.id = favorites.contentId AND favorites.contentType = 'LIVE'
        WHERE (:categoryId IS NULL OR channels.categoryId = :categoryId)
        ORDER BY channels.channelNumber
        """
    )
    fun observeChannels(categoryId: String?): Flow<List<ChannelWithFavorite>>

    @Query(
        """
        SELECT channels.id AS id, channels.name AS name, channels.logoUrl AS logoUrl,
               channels.categoryId AS categoryId, channels.channelNumber AS channelNumber, 1 AS isFavorite
        FROM channels
        INNER JOIN favorites ON channels.id = favorites.contentId AND favorites.contentType = 'LIVE'
        ORDER BY favorites.addedAtEpochMillis DESC
        """
    )
    fun observeFavoriteChannels(): Flow<List<ChannelWithFavorite>>

    @Query("SELECT * FROM channels ORDER BY channelNumber")
    suspend fun getCachedChannels(): List<ChannelEntity>

    @Query("SELECT contentId FROM favorites WHERE contentType = 'LIVE' ORDER BY addedAtEpochMillis DESC")
    suspend fun getFavoriteChannelIds(): List<String>

    @Query("SELECT name FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannelName(channelId: String): String?

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Query("DELETE FROM categories WHERE contentType = 'LIVE'")
    suspend fun clearCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>, channels: List<ChannelEntity>) {
        clearCategories()
        clearChannels()
        insertCategories(categories)
        insertChannels(channels)
    }
}
