package com.sirktv.app.network

import com.sirktv.app.network.dto.VodInfoResponseDto
import com.sirktv.app.network.dto.VodStreamDto
import com.sirktv.app.network.dto.XtreamCategoryDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface XtreamVodApiService {

    @GET
    suspend fun getVodCategories(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<XtreamCategoryDto>

    @GET
    suspend fun getVodStreams(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams"
    ): List<VodStreamDto>

    @GET
    suspend fun getVodInfo(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("vod_id") vodId: String,
        @Query("action") action: String = "get_vod_info"
    ): VodInfoResponseDto
}
