package com.sirktv.app.network

import java.net.URI

object XtreamUrlBuilder {

    /**
     * Normalizes a user-entered server address (e.g. "myserver.com:8080",
     * "http://myserver.com/", "https://myserver.com:8443") into a full
     * player_api.php URL. Throws [IllegalArgumentException] with a
     * user-presentable message on malformed input.
     */
    fun buildPlayerApiUrl(rawServerUrl: String): String {
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

        return "$withScheme/player_api.php"
    }
}
