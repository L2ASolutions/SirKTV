package com.sirktv.app.storage.db

import androidx.room.Entity

/**
 * Generic across content types (channels, movies, series). [title]/[imageUrl]
 * are denormalized snapshots for the Favorites screen, same reasoning as
 * WatchProgressEntity — render instantly without a catalog join.
 */
@Entity(tableName = "favorites", primaryKeys = ["contentId", "contentType"])
data class FavoriteEntity(
    val contentId: String,
    val contentType: String,
    val title: String,
    val imageUrl: String?,
    val sortOrder: Int,
    val addedAtEpochMillis: Long
)
