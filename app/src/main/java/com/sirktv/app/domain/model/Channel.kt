package com.sirktv.app.domain.model

data class Channel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val categoryId: String,
    val channelNumber: Int,
    val isFavorite: Boolean
)
