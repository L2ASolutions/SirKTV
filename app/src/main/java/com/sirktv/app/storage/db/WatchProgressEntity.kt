package com.sirktv.app.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val contentId: String,
    val contentType: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtEpochMillis: Long
)
