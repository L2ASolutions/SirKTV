package com.sirktv.app.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String,
    val rating: Float?,
    val cast: String?,
    val director: String?,
    val synopsis: String?,
    /** Xtream's last_modified for this series, epoch millis; 0 when the panel doesn't provide one. */
    val lastModifiedEpochMillis: Long = 0L
)
