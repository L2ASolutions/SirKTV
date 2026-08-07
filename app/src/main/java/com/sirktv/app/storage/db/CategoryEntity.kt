package com.sirktv.app.storage.db

import androidx.room.Entity

/** One shared table across Live/VOD/Series — [contentType] disambiguates ids that can collide across catalogs. */
@Entity(tableName = "categories", primaryKeys = ["id", "contentType"])
data class CategoryEntity(
    val id: String,
    val name: String,
    val contentType: String
)
