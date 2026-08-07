package com.sirktv.app.di

import android.content.Context
import androidx.room.Room
import com.sirktv.app.storage.db.AppDatabase
import com.sirktv.app.storage.db.ChannelDao
import com.sirktv.app.storage.db.FavoriteDao
import com.sirktv.app.storage.db.MovieDao
import com.sirktv.app.storage.db.SearchHistoryDao
import com.sirktv.app.storage.db.SeriesDao
import com.sirktv.app.storage.db.WatchProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "sirktv.db")
            // Pre-release app, no shipped user data yet — destructive fallback
            // is the right call while the schema (v1 -> v2 added movies/series/
            // watch progress/search history and made favorites generic) is
            // still under active development. Revisit once there's real data
            // on real devices to preserve across an update.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideChannelDao(database: AppDatabase): ChannelDao = database.channelDao()

    @Provides
    @Singleton
    fun provideMovieDao(database: AppDatabase): MovieDao = database.movieDao()

    @Provides
    @Singleton
    fun provideSeriesDao(database: AppDatabase): SeriesDao = database.seriesDao()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    @Singleton
    fun provideWatchProgressDao(database: AppDatabase): WatchProgressDao = database.watchProgressDao()

    @Provides
    @Singleton
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao = database.searchHistoryDao()
}
