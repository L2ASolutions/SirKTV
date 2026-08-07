package com.sirktv.app.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Shared shape for get_vod_categories / get_series_categories (identical to LiveCategoryDto, kept separate to avoid touching the Phase 2 live-TV contract). */
@Serializable
data class XtreamCategoryDto(
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null
)
