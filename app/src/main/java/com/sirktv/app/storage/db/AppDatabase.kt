package com.sirktv.app.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ChannelEntity::class,
        FavoriteEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        WatchProgressEntity::class,
        SearchHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchProgressDao(): WatchProgressDao
    abstract fun searchHistoryDao(): SearchHistoryDao
}
