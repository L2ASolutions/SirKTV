package com.sirktv.app.domain.model

data class FavoriteItem(
    val contentId: String,
    val contentType: ContentType,
    val title: String,
    val imageUrl: String?,
    val sortOrder: Int,
    val addedAtEpochMillis: Long
)

enum class FavoriteSort { ALPHABETICAL, RECENTLY_ADDED, CUSTOM }
