package com.sirktv.app.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String,
    val rating: Float?,
    val containerExtension: String,
    val addedAtEpochMillis: Long
)
