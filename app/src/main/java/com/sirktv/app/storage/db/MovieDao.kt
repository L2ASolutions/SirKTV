package com.sirktv.app.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class MovieWithFavorite(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String,
    val rating: Float?,
    val containerExtension: String,
    val isFavorite: Boolean
)

@Dao
interface MovieDao {

    @Query("SELECT * FROM categories WHERE contentType = 'VOD' ORDER BY name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT movies.id AS id, movies.title AS title, movies.posterUrl AS posterUrl,
               movies.categoryId AS categoryId, movies.rating AS rating, movies.containerExtension AS containerExtension,
               CASE WHEN favorites.contentId IS NULL THEN 0 ELSE 1 END AS isFavorite
        FROM movies
        LEFT JOIN favorites ON movies.id = favorites.contentId AND favorites.contentType = 'MOVIE'
        WHERE (:categoryId IS NULL OR movies.categoryId = :categoryId)
        ORDER BY movies.title
        """
    )
    fun observeMovies(categoryId: String?): Flow<List<MovieWithFavorite>>

    @Query(
        """
        SELECT movies.id AS id, movies.title AS title, movies.posterUrl AS posterUrl,
               movies.categoryId AS categoryId, movies.rating AS rating, movies.containerExtension AS containerExtension, 1 AS isFavorite
        FROM movies
        INNER JOIN favorites ON movies.id = favorites.contentId AND favorites.contentType = 'MOVIE'
        ORDER BY favorites.addedAtEpochMillis DESC
        """
    )
    fun observeFavoriteMovies(): Flow<List<MovieWithFavorite>>

    @Query("SELECT * FROM movies ORDER BY title")
    suspend fun getCachedMovies(): List<MovieEntity>

    @Query("SELECT title, posterUrl FROM movies WHERE id = :movieId LIMIT 1")
    suspend fun getMovieTitleAndPoster(movieId: String): MovieTitleAndPoster?

    @Query("DELETE FROM movies")
    suspend fun clearMovies()

    @Query("DELETE FROM categories WHERE contentType = 'VOD'")
    suspend fun clearCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>, movies: List<MovieEntity>) {
        clearCategories()
        clearMovies()
        insertCategories(categories)
        insertMovies(movies)
    }
}

data class MovieTitleAndPoster(val title: String, val posterUrl: String?)
