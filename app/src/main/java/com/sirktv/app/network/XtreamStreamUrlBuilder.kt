package com.sirktv.app.network

/**
 * Builds Xtream live-stream playback URLs. Xtream has no notion of a CDN
 * failover URL, but it does conventionally serve the same channel over both
 * HLS (`.m3u8`) and raw MPEG-TS (`.ts`) — the primary/backup fallback in the
 * playback engineering spec maps onto that: HLS first, MPEG-TS as the backup
 * if the HLS source fails to load.
 */
object XtreamStreamUrlBuilder {

    fun buildPrimaryUrl(serverUrl: String, username: String, password: String, streamId: String): String =
        buildLiveStreamUrl(serverUrl, username, password, streamId, extension = "m3u8")

    fun buildBackupUrl(serverUrl: String, username: String, password: String, streamId: String): String =
        buildLiveStreamUrl(serverUrl, username, password, streamId, extension = "ts")

    private fun buildLiveStreamUrl(
        serverUrl: String,
        username: String,
        password: String,
        streamId: String,
        extension: String
    ): String {
        val base = XtreamUrlBuilder.normalizeBase(serverUrl)
        return "$base/live/$username/$password/$streamId.$extension"
    }
}
