package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.MovieDetail
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeMovies(categoryId: String? = null): Flow<List<Movie>>
    fun observeFavoriteMovies(): Flow<List<Movie>>

    suspend fun getCachedMovies(): List<Movie>
    suspend fun toggleFavorite(movieId: String)
    suspend fun sync()
    suspend fun getMovieDetail(movieId: String): MovieDetail?
}
