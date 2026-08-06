package com.sirktv.app.network

import com.sirktv.app.network.dto.XtreamAuthResponseDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface XtreamApiService {

    /**
     * Xtream Codes servers are user-supplied at login time, so the full
     * player_api.php URL is passed per-call rather than via a fixed Retrofit
     * base URL. Calling this endpoint with just username/password (no
     * `action` query param) is the Xtream Codes convention for authenticating
     * and fetching account/server info.
     */
    @GET
    suspend fun authenticate(
        @Url playerApiUrl: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): XtreamAuthResponseDto
}
