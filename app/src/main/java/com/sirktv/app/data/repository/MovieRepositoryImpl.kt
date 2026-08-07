package com.sirktv.app.data.repository

import com.sirktv.app.data.mapper.MovieMapper
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.MovieDetail
import com.sirktv.app.domain.repository.MovieRepository
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.network.XtreamUrlBuilder
import com.sirktv.app.network.XtreamVodApiService
import com.sirktv.app.storage.db.FavoriteDao
import com.sirktv.app.storage.db.FavoriteEntity
import com.sirktv.app.storage.db.MovieDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val apiService: XtreamVodApiService,
    private val movieDao: MovieDao,
    private val favoriteDao: FavoriteDao,
    private val currentSession: CurrentSession
) : MovieRepository {

    private val contentType = ContentType.MOVIE.name

    override fun observeCategories(): Flow<List<Category>> =
        movieDao.observeCategories().map { list -> list.map(MovieMapper::toCategory) }

    override fun observeMovies(categoryId: String?): Flow<List<Movie>> =
        movieDao.observeMovies(categoryId).map { list -> list.map(MovieMapper::toMovie) }

    override fun observeFavoriteMovies(): Flow<List<Movie>> =
        movieDao.observeFavoriteMovies().map { list -> list.map(MovieMapper::toMovie) }

    override suspend fun getCachedMovies(): List<Movie> {
        val favoriteIds = favoriteDao.listCustomOrder(contentType).map { it.contentId }.toSet()
        return movieDao.getCachedMovies().map { entity ->
            Movie(
                id = entity.id,
                title = entity.title,
                posterUrl = entity.posterUrl,
                categoryId = entity.categoryId,
                rating = entity.rating,
                containerExtension = entity.containerExtension,
                isFavorite = entity.id in favoriteIds
            )
        }
    }

    override suspend fun toggleFavorite(movieId: String) {
        if (favoriteDao.isFavorite(movieId, contentType)) {
            favoriteDao.delete(movieId, contentType)
        } else {
            val movie = movieDao.getMovieTitleAndPoster(movieId)
            val sortOrder = favoriteDao.maxSortOrder(contentType) + 1
            favoriteDao.insert(
                FavoriteEntity(
                    contentId = movieId,
                    contentType = contentType,
                    title = movie?.title ?: movieId,
                    imageUrl = movie?.posterUrl,
                    sortOrder = sortOrder,
                    addedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun sync() {
        val credentials = currentSession.credentials.value ?: return
        runCatching {
            val playerApiUrl = XtreamUrlBuilder.buildPlayerApiUrl(credentials.serverUrl)
            val categoriesDto = apiService.getVodCategories(playerApiUrl, credentials.username, credentials.password)
            val streamsDto = apiService.getVodStreams(playerApiUrl, credentials.username, credentials.password)
            val now = System.currentTimeMillis()
            val categories = categoriesDto.mapNotNull(MovieMapper::toCategoryEntity)
            val movies = streamsDto.mapNotNull { MovieMapper.toMovieEntity(it, now) }
            movieDao.replaceAll(categories, movies)
        }
    }

    override suspend fun getMovieDetail(movieId: String): MovieDetail? {
        val credentials = currentSession.credentials.value ?: return null
        val playerApiUrl = runCatching { XtreamUrlBuilder.buildPlayerApiUrl(credentials.serverUrl) }.getOrElse { return null }
        return runCatching {
            val response = apiService.getVodInfo(playerApiUrl, credentials.username, credentials.password, movieId)
            MovieMapper.toMovieDetail(response)
        }.getOrNull()
    }
}
