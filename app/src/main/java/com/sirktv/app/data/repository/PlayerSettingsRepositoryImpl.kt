package com.sirktv.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sirktv.app.di.PlayerSettingsDataStore
import com.sirktv.app.domain.model.BufferProfile
import com.sirktv.app.domain.model.PlayerSettings
import com.sirktv.app.domain.model.StreamQuality
import com.sirktv.app.domain.repository.PlayerSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlayerSettingsRepositoryImpl @Inject constructor(
    @PlayerSettingsDataStore private val dataStore: DataStore<Preferences>
) : PlayerSettingsRepository {

    override fun observe(): Flow<PlayerSettings> = dataStore.data.map { it.toPlayerSettings() }

    override suspend fun update(settings: PlayerSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.BUFFER_PROFILE] = settings.bufferProfile.name
            prefs[Keys.CUSTOM_MIN] = settings.customMinBufferMs
            prefs[Keys.CUSTOM_MAX] = settings.customMaxBufferMs
            prefs[Keys.CUSTOM_START] = settings.customStartBufferMs
            prefs[Keys.CUSTOM_REBUFFER] = settings.customRebufferThresholdMs
            prefs[Keys.QUALITY] = settings.preferredQuality.name
            prefs[Keys.HW_DECODE] = settings.hardwareDecodingEnabled
            prefs[Keys.RECONNECT_ATTEMPTS] = settings.reconnectAttempts
            prefs[Keys.SHOW_BUFFER_HEALTH] = settings.showBufferHealth
            prefs[Keys.USER_AGENT] = settings.userAgent
        }
    }

    private fun Preferences.toPlayerSettings(): PlayerSettings {
        val defaults = PlayerSettings()
        return PlayerSettings(
            bufferProfile = this[Keys.BUFFER_PROFILE]?.let { runCatching { BufferProfile.valueOf(it) }.getOrNull() }
                ?: defaults.bufferProfile,
            customMinBufferMs = this[Keys.CUSTOM_MIN] ?: defaults.customMinBufferMs,
            customMaxBufferMs = this[Keys.CUSTOM_MAX] ?: defaults.customMaxBufferMs,
            customStartBufferMs = this[Keys.CUSTOM_START] ?: defaults.customStartBufferMs,
            customRebufferThresholdMs = this[Keys.CUSTOM_REBUFFER] ?: defaults.customRebufferThresholdMs,
            preferredQuality = this[Keys.QUALITY]?.let { runCatching { StreamQuality.valueOf(it) }.getOrNull() }
                ?: defaults.preferredQuality,
            hardwareDecodingEnabled = this[Keys.HW_DECODE] ?: defaults.hardwareDecodingEnabled,
            reconnectAttempts = this[Keys.RECONNECT_ATTEMPTS] ?: defaults.reconnectAttempts,
            showBufferHealth = this[Keys.SHOW_BUFFER_HEALTH] ?: defaults.showBufferHealth,
            userAgent = this[Keys.USER_AGENT] ?: defaults.userAgent
        )
    }

    private object Keys {
        val BUFFER_PROFILE = stringPreferencesKey("buffer_profile")
        val CUSTOM_MIN = intPreferencesKey("custom_min_buffer_ms")
        val CUSTOM_MAX = intPreferencesKey("custom_max_buffer_ms")
        val CUSTOM_START = intPreferencesKey("custom_start_buffer_ms")
        val CUSTOM_REBUFFER = intPreferencesKey("custom_rebuffer_threshold_ms")
        val QUALITY = stringPreferencesKey("preferred_quality")
        val HW_DECODE = booleanPreferencesKey("hardware_decoding_enabled")
        val RECONNECT_ATTEMPTS = intPreferencesKey("reconnect_attempts")
        val SHOW_BUFFER_HEALTH = booleanPreferencesKey("show_buffer_health")
        val USER_AGENT = stringPreferencesKey("user_agent")
    }
}
