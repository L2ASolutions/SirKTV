package com.sirktv.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)
