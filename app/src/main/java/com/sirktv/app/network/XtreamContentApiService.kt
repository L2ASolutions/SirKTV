package com.sirktv.app.network

import com.sirktv.app.network.dto.EpgResponseDto
import com.sirktv.app.network.dto.LiveCategoryDto
import com.sirktv.app.network.dto.LiveStreamDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface XtreamContentApiService {

    @GET
    suspend fun getLiveCategories(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_categories"
    ): List<LiveCategoryDto>

    @GET
    suspend fun getLiveStreams(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_live_streams"
    ): List<LiveStreamDto>

    @GET
    suspend fun getShortEpg(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("stream_id") streamId: String,
        @Query("limit") limit: Int = 4,
        @Query("action") action: String = "get_short_epg"
    ): EpgResponseDto
}
