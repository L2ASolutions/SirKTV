package com.sirktv.app.network

import com.sirktv.app.network.dto.SeriesDto
import com.sirktv.app.network.dto.SeriesInfoResponseDto
import com.sirktv.app.network.dto.XtreamCategoryDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface XtreamSeriesApiService {

    @GET
    suspend fun getSeriesCategories(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series_categories"
    ): List<XtreamCategoryDto>

    @GET
    suspend fun getSeriesList(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_series"
    ): List<SeriesDto>

    @GET
    suspend fun getSeriesInfo(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("series_id") seriesId: String,
        @Query("action") action: String = "get_series_info"
    ): SeriesInfoResponseDto
}
