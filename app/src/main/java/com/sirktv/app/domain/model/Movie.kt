package com.sirktv.app.domain.model

data class Movie(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String,
    val rating: Float?,
    val containerExtension: String,
    val isFavorite: Boolean,
    val addedAtEpochMillis: Long = 0L
)

/**
 * Fetched on demand when opening Movie Detail — not synced/cached with the
 * rest of the catalog, since Xtream only returns synopsis/cast/director via a
 * separate get_vod_info call per title, and prefetching that for an entire
 * catalog would be hundreds of extra requests nobody asked for.
 */
data class MovieDetail(
    val synopsis: String?,
    val cast: List<String>,
    val director: String?,
    val durationMinutes: Int?,
    val genre: String?
)
