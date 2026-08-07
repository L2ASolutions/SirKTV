package com.sirktv.app.domain.model

/**
 * One denormalized row per piece of content ever played — deliberately not a
 * join back to Channel/Movie/Episode tables. Continue Watching and Recently
 * Watched need to render instantly without resolving a live catalog lookup
 * for content that may since have left the provider's catalog entirely.
 */
data class WatchProgress(
    val contentId: String,
    val contentType: ContentType,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtEpochMillis: Long
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
