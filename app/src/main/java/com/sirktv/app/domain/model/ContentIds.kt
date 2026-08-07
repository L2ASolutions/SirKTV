package com.sirktv.app.domain.model

/**
 * A movie's content id is just its Xtream stream id. An episode has no
 * single natural id shared between "what to fetch" (series id) and "which
 * episode" (season/number), so watch progress / favorites / navigation all
 * key episodes off one encoded string instead of three separate columns.
 */
object ContentIds {
    fun forEpisode(seriesId: String, seasonNumber: Int, episodeNumber: Int): String =
        "$seriesId:$seasonNumber:$episodeNumber"

    fun parseEpisode(contentId: String): Triple<String, Int, Int>? {
        val parts = contentId.split(":")
        if (parts.size != 3) return null
        val season = parts[1].toIntOrNull() ?: return null
        val episode = parts[2].toIntOrNull() ?: return null
        return Triple(parts[0], season, episode)
    }
}
