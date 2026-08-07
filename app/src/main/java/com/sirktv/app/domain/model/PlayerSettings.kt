package com.sirktv.app.domain.model

enum class BufferProfile { LOW, MEDIUM, HIGH, CUSTOM }

enum class StreamQuality { AUTO, P1080, P720, P480 }

data class BufferMillis(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val startBufferMs: Int,
    val rebufferThresholdMs: Int
)

data class PlayerSettings(
    val bufferProfile: BufferProfile = BufferProfile.MEDIUM,
    val customMinBufferMs: Int = MEDIUM_BUFFER.minBufferMs,
    val customMaxBufferMs: Int = MEDIUM_BUFFER.maxBufferMs,
    val customStartBufferMs: Int = MEDIUM_BUFFER.startBufferMs,
    val customRebufferThresholdMs: Int = MEDIUM_BUFFER.rebufferThresholdMs,
    val preferredQuality: StreamQuality = StreamQuality.AUTO,
    val hardwareDecodingEnabled: Boolean = true,
    val reconnectAttempts: Int = 5,
    val showBufferHealth: Boolean = false,
    val userAgent: String = "SirKTV/1.0 (Android TV)"
) {
    companion object {
        val LOW_BUFFER = BufferMillis(minBufferMs = 8_000, maxBufferMs = 25_000, startBufferMs = 1_500, rebufferThresholdMs = 3_000)
        val MEDIUM_BUFFER = BufferMillis(minBufferMs = 15_000, maxBufferMs = 50_000, startBufferMs = 2_500, rebufferThresholdMs = 5_000)
        val HIGH_BUFFER = BufferMillis(minBufferMs = 25_000, maxBufferMs = 90_000, startBufferMs = 4_000, rebufferThresholdMs = 8_000)
    }

    fun resolveBufferMillis(): BufferMillis = when (bufferProfile) {
        BufferProfile.LOW -> LOW_BUFFER
        BufferProfile.MEDIUM -> MEDIUM_BUFFER
        BufferProfile.HIGH -> HIGH_BUFFER
        BufferProfile.CUSTOM -> BufferMillis(
            minBufferMs = customMinBufferMs,
            maxBufferMs = customMaxBufferMs,
            startBufferMs = customStartBufferMs,
            rebufferThresholdMs = customRebufferThresholdMs
        )
    }
}
