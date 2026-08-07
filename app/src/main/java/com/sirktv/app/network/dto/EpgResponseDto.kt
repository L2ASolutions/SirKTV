package com.sirktv.app.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpgResponseDto(
    @SerialName("epg_listings") val epgListings: List<EpgListingDto>? = null
)

/** [title] and [description] are base64-encoded per the Xtream Codes convention. */
@Serializable
data class EpgListingDto(
    val title: String? = null,
    val description: String? = null,
    @SerialName("start_timestamp") val startTimestamp: String? = null,
    @SerialName("stop_timestamp") val stopTimestamp: String? = null
)
