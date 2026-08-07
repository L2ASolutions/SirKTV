package com.sirktv.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sirktv.app.di.StartupPreferenceDataStore
import com.sirktv.app.domain.model.StartupPreference
import com.sirktv.app.domain.repository.StartupPreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StartupPreferenceRepositoryImpl @Inject constructor(
    @StartupPreferenceDataStore private val dataStore: DataStore<Preferences>
) : StartupPreferenceRepository {

    override fun observe(): Flow<StartupPreference> = dataStore.data.map { it.toStartupPreference() }

    override suspend fun get(): StartupPreference = dataStore.data.first().toStartupPreference()

    override suspend fun update(preference: StartupPreference) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_START] = preference.autoStartLiveTv
            if (preference.startupChannelId != null) {
                prefs[Keys.STARTUP_CHANNEL_ID] = preference.startupChannelId
            } else {
                prefs.remove(Keys.STARTUP_CHANNEL_ID)
            }
            prefs[Keys.RESUME_LAST] = preference.resumeLastChannel
            if (preference.lastWatchedChannelId != null) {
                prefs[Keys.LAST_WATCHED_ID] = preference.lastWatchedChannelId
            } else {
                prefs.remove(Keys.LAST_WATCHED_ID)
            }
        }
    }

    override suspend fun setLastWatchedChannel(channelId: String) {
        dataStore.edit { prefs -> prefs[Keys.LAST_WATCHED_ID] = channelId }
    }

    private fun Preferences.toStartupPreference(): StartupPreference {
        val defaults = StartupPreference()
        return StartupPreference(
            autoStartLiveTv = this[Keys.AUTO_START] ?: defaults.autoStartLiveTv,
            startupChannelId = this[Keys.STARTUP_CHANNEL_ID],
            resumeLastChannel = this[Keys.RESUME_LAST] ?: defaults.resumeLastChannel,
            lastWatchedChannelId = this[Keys.LAST_WATCHED_ID]
        )
    }

    private object Keys {
        val AUTO_START = booleanPreferencesKey("auto_start_live_tv")
        val STARTUP_CHANNEL_ID = stringPreferencesKey("startup_channel_id")
        val RESUME_LAST = booleanPreferencesKey("resume_last_channel")
        val LAST_WATCHED_ID = stringPreferencesKey("last_watched_channel_id")
    }
}
