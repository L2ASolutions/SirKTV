package com.sirktv.app.domain.model

/**
 * Unlike movies, Xtream's get_series catalog endpoint already includes
 * synopsis/cast/director inline — no separate detail call needed just to
 * browse or show a Series Detail header.
 */
data class Series(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val categoryId: String,
    val rating: Float?,
    val cast: List<String>,
    val director: String?,
    val synopsis: String?,
    val isFavorite: Boolean,
    val lastModifiedEpochMillis: Long = 0L
)

data class Episode(
    val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val durationMinutes: Int?,
    val containerExtension: String,
    val synopsis: String?
)

data class SeasonInfo(
    val seasonNumber: Int,
    val episodes: List<Episode>
)

/** Fetched on demand when opening Series Detail — episodes aren't part of the synced catalog. */
data class SeriesInfo(
    val seasons: List<SeasonInfo>
) {
    fun episode(seasonNumber: Int, episodeNumber: Int): Episode? =
        seasons.find { it.seasonNumber == seasonNumber }?.episodes?.find { it.episodeNumber == episodeNumber }

    fun nextEpisode(seasonNumber: Int, episodeNumber: Int): Episode? {
        val season = seasons.find { it.seasonNumber == seasonNumber } ?: return null
        val withinSeason = season.episodes.find { it.episodeNumber == episodeNumber + 1 }
        if (withinSeason != null) return withinSeason
        val nextSeason = seasons.find { it.seasonNumber == seasonNumber + 1 } ?: return null
        return nextSeason.episodes.minByOrNull { it.episodeNumber }
    }
}
