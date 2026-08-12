package com.sirktv.app.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache of get_series_info's episode payload, keyed by [seriesId] so a whole
 * series' episodes can be cleared/counted together. Unlike the synced movie/
 * series/channel catalogs, this is populated lazily — one series at a time,
 * the first time its detail screen is opened — rather than by a section-wide
 * sync, since get_series_info is a per-title call.
 */
@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val durationMinutes: Int?,
    val containerExtension: String,
    val synopsis: String?
)
