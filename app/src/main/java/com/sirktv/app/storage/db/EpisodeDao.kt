package com.sirktv.app.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY seasonNumber, episodeNumber")
    suspend fun getEpisodesForSeries(seriesId: String): List<EpisodeEntity>

    @Query("SELECT COUNT(*) FROM episodes WHERE seriesId = :seriesId")
    suspend fun getEpisodeCount(seriesId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("DELETE FROM episodes WHERE seriesId = :seriesId")
    suspend fun clearForSeries(seriesId: String)
}
