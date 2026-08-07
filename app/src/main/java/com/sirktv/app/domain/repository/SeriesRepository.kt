package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.model.SeriesInfo
import kotlinx.coroutines.flow.Flow

interface SeriesRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeSeries(categoryId: String? = null): Flow<List<Series>>
    fun observeFavoriteSeries(): Flow<List<Series>>

    suspend fun getCachedSeries(): List<Series>
    suspend fun toggleFavorite(seriesId: String)
    suspend fun sync()
    suspend fun getSeriesInfo(seriesId: String): SeriesInfo?
}
