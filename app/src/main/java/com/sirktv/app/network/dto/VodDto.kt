package com.sirktv.app.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VodStreamDto(
    @SerialName("stream_id") val streamId: Int? = null,
    val name: String? = null,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    val rating: String? = null,
    // Xtream's "date this was added to the panel" — epoch seconds as a
    // string. Some panels omit or zero this out, so it's read defensively
    // and the mapper falls back to stream_id ordering when it's missing.
    val added: String? = null
)

@Serializable
data class VodInfoResponseDto(
    val info: VodInfoDetailsDto? = null
)

/** Panels are inconsistent about plot vs description, cast vs actors — both are read defensively in the mapper. */
@Serializable
data class VodInfoDetailsDto(
    val plot: String? = null,
    val description: String? = null,
    val cast: String? = null,
    val actors: String? = null,
    val director: String? = null,
    val genre: String? = null,
    @SerialName("duration_secs") val durationSecs: Int? = null,
    val duration: String? = null
)
