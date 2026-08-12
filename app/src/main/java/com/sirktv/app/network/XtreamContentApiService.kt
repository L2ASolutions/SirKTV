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

    /**
     * The "full" counterpart to [getShortEpg] — same response shape
     * (`epg_listings`, same base64 title/description convention decoded by
     * [com.sirktv.app.data.mapper.EpgMapper]) but not bounded by a `limit`
     * param, so a panel that supports it returns the whole day's schedule
     * instead of a short window of upcoming programs.
     */
    @GET
    suspend fun getFullEpg(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("stream_id") streamId: String,
        @Query("action") action: String = "get_simple_data_table"
    ): EpgResponseDto
}
