package com.sirktv.app.data.repository

import com.sirktv.app.data.mapper.SeriesMapper
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.model.SeriesInfo
import com.sirktv.app.domain.repository.SeriesRepository
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.network.XtreamSeriesApiService
import com.sirktv.app.network.XtreamUrlBuilder
import com.sirktv.app.storage.db.FavoriteDao
import com.sirktv.app.storage.db.FavoriteEntity
import com.sirktv.app.storage.db.SeriesDao
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SeriesRepositoryImpl @Inject constructor(
    private val apiService: XtreamSeriesApiService,
    private val seriesDao: SeriesDao,
    private val favoriteDao: FavoriteDao,
    private val currentSession: CurrentSession
) : SeriesRepository {

    private val contentType = ContentType.SERIES.name

    override fun observeCategories(): Flow<List<Category>> =
        seriesDao.observeCategories().map { list -> list.map(SeriesMapper::toCategory) }

    override fun observeSeries(categoryId: String?): Flow<List<Series>> =
        seriesDao.observeSeries(categoryId).map { list -> list.map(SeriesMapper::toSeries) }

    override fun observeFavoriteSeries(): Flow<List<Series>> =
        seriesDao.observeFavoriteSeries().map { list -> list.map(SeriesMapper::toSeries) }

    override suspend fun getCachedSeries(): List<Series> {
        val favoriteIds = favoriteDao.listCustomOrder(contentType).map { it.contentId }.toSet()
        return seriesDao.getCachedSeries().map { entity ->
            Series(
                id = entity.id,
                title = entity.title,
                posterUrl = entity.posterUrl,
                categoryId = entity.categoryId,
                rating = entity.rating,
                cast = entity.cast?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
                director = entity.director,
                synopsis = entity.synopsis,
                lastModifiedEpochMillis = entity.lastModifiedEpochMillis,
                isFavorite = entity.id in favoriteIds
            )
        }
    }

    override suspend fun toggleFavorite(seriesId: String) {
        if (favoriteDao.isFavorite(seriesId, contentType)) {
            favoriteDao.delete(seriesId, contentType)
        } else {
            val series = seriesDao.getSeriesTitleAndPoster(seriesId)
            val sortOrder = favoriteDao.maxSortOrder(contentType) + 1
            favoriteDao.insert(
                FavoriteEntity(
                    contentId = seriesId,
                    contentType = contentType,
                    title = series?.title ?: seriesId,
                    imageUrl = series?.posterUrl,
                    sortOrder = sortOrder,
                    addedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun sync(): Result<Unit> {
        val credentials = currentSession.credentials.value
            ?: return Result.failure(IllegalStateException("Not signed in"))
        return runCatching {
            val playerApiUrl = XtreamUrlBuilder.buildPlayerApiUrl(credentials.serverUrl)
            coroutineScope {
                val categoriesDto = async { apiService.getSeriesCategories(playerApiUrl, credentials.username, credentials.password) }
                val seriesDto = async { apiService.getSeriesList(playerApiUrl, credentials.username, credentials.password) }
                val categories = categoriesDto.await().mapNotNull(SeriesMapper::toCategoryEntity)
                val series = seriesDto.await().mapNotNull(SeriesMapper::toSeriesEntity)
                seriesDao.replaceAll(categories, series)
            }
        }
    }

    override suspend fun getSeriesInfo(seriesId: String): SeriesInfo? {
        val credentials = currentSession.credentials.value ?: return null
        val playerApiUrl = runCatching { XtreamUrlBuilder.buildPlayerApiUrl(credentials.serverUrl) }.getOrElse { return null }
        return runCatching {
            val response = apiService.getSeriesInfo(playerApiUrl, credentials.username, credentials.password, seriesId)
            SeriesMapper.toSeriesInfo(seriesId, response)
        }.getOrNull()
    }
}
