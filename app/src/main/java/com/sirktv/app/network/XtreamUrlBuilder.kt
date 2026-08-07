package com.sirktv.app.network

import java.net.URI

object XtreamUrlBuilder {

    /**
     * Normalizes a user-entered server address (e.g. "myserver.com:8080",
     * "http://myserver.com/", "https://myserver.com:8443") into a scheme+host
     * base with no trailing slash and no path. Throws [IllegalArgumentException]
     * with a user-presentable message on malformed input.
     */
    fun normalizeBase(rawServerUrl: String): String {
        val trimmed = rawServerUrl.trim().trimEnd('/')
        require(trimmed.isNotEmpty()) { "Enter your server address" }

        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }

        val uri = try {
            URI(withScheme)
        } catch (e: Exception) {
            throw IllegalArgumentException("Enter a valid server address, e.g. http://example.com:8080", e)
        }

        if (uri.host.isNullOrBlank()) {
            throw IllegalArgumentException("Enter a valid server address, e.g. http://example.com:8080")
        }

        return withScheme
    }

    /** Full player_api.php URL, e.g. "http://example.com:8080/player_api.php". */
    fun buildPlayerApiUrl(rawServerUrl: String): String = "${normalizeBase(rawServerUrl)}/player_api.php"
}
