package com.sirktv.app.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class SeriesWithFavorite(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String,
    val rating: Float?,
    val cast: String?,
    val director: String?,
    val synopsis: String?,
    val isFavorite: Boolean
)

@Dao
interface SeriesDao {

    @Query("SELECT * FROM categories WHERE contentType = 'SERIES' ORDER BY name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT series.id AS id, series.title AS title, series.posterUrl AS posterUrl, series.categoryId AS categoryId,
               series.rating AS rating, series.`cast` AS `cast`, series.director AS director, series.synopsis AS synopsis,
               CASE WHEN favorites.contentId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM series
        LEFT JOIN favorites ON series.id = favorites.contentId AND favorites.contentType = 'SERIES'
        WHERE (:categoryId IS NULL OR series.categoryId = :categoryId)
        ORDER BY series.title
        """
    )
    fun observeSeries(categoryId: String?): Flow<List<SeriesWithFavorite>>

    @Query(
        """
        SELECT series.id AS id, series.title AS title, series.posterUrl AS posterUrl, series.categoryId AS categoryId,
               series.rating AS rating, series.`cast` AS `cast`, series.director AS director, series.synopsis AS synopsis, 1 AS isFavorite
        FROM series
        INNER JOIN favorites ON series.id = favorites.contentId AND favorites.contentType = 'SERIES'
        ORDER BY favorites.addedAtEpochMillis DESC
        """
    )
    fun observeFavoriteSeries(): Flow<List<SeriesWithFavorite>>

    @Query("SELECT * FROM series ORDER BY title")
    suspend fun getCachedSeries(): List<SeriesEntity>

    @Query("SELECT title, posterUrl FROM series WHERE id = :seriesId LIMIT 1")
    suspend fun getSeriesTitleAndPoster(seriesId: String): SeriesTitleAndPoster?

    @Query("DELETE FROM series")
    suspend fun clearSeries()

    @Query("DELETE FROM categories WHERE contentType = 'SERIES'")
    suspend fun clearCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: List<SeriesEntity>)

    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>, series: List<SeriesEntity>) {
        clearCategories()
        clearSeries()
        insertCategories(categories)
        insertSeries(series)
    }
}

data class SeriesTitleAndPoster(val title: String, val posterUrl: String?)
