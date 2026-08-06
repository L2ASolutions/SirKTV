package com.sirktv.app.domain.model

data class UserProfile(
    val username: String,
    val serverUrl: String,
    val status: SubscriptionStatus,
    val expiresAtEpochSeconds: Long?,
    val isTrial: Boolean,
    val activeConnections: Int,
    val maxConnections: Int,
    val allowedOutputFormats: List<String>
)
