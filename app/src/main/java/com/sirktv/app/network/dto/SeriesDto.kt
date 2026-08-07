package com.sirktv.app.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeriesDto(
    @SerialName("series_id") val seriesId: Int? = null,
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val rating: String? = null
)

/** Episode map keys are season numbers, as strings — e.g. {"1": [...], "2": [...]}. */
@Serializable
data class SeriesInfoResponseDto(
    val episodes: Map<String, List<SeriesEpisodeDto>>? = null
)

@Serializable
data class SeriesEpisodeDto(
    val id: String? = null,
    @SerialName("episode_num") val episodeNum: Int? = null,
    val title: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    val info: SeriesEpisodeInfoDto? = null
)

@Serializable
data class SeriesEpisodeInfoDto(
    @SerialName("duration_secs") val durationSecs: Int? = null,
    val plot: String? = null
)
