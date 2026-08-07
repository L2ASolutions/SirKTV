package com.sirktv.app.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.sirktv.app.BuildConfig
import com.sirktv.app.network.XtreamApiService
import com.sirktv.app.network.XtreamContentApiService
import com.sirktv.app.network.XtreamSeriesApiService
import com.sirktv.app.network.XtreamVodApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            // Placeholder base URL: every call supplies its own full @Url,
            // since Xtream servers are entered by the user at login time.
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideXtreamApiService(retrofit: Retrofit): XtreamApiService =
        retrofit.create(XtreamApiService::class.java)

    @Provides
    @Singleton
    fun provideXtreamContentApiService(retrofit: Retrofit): XtreamContentApiService =
        retrofit.create(XtreamContentApiService::class.java)

    @Provides
    @Singleton
    fun provideXtreamVodApiService(retrofit: Retrofit): XtreamVodApiService =
        retrofit.create(XtreamVodApiService::class.java)

    @Provides
    @Singleton
    fun provideXtreamSeriesApiService(retrofit: Retrofit): XtreamSeriesApiService =
        retrofit.create(XtreamSeriesApiService::class.java)
}
