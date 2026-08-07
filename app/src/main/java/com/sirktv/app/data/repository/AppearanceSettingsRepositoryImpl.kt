package com.sirktv.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sirktv.app.di.AppearanceSettingsDataStore
import com.sirktv.app.domain.model.AppearanceSettings
import com.sirktv.app.domain.model.FocusStyle
import com.sirktv.app.domain.model.TextScale
import com.sirktv.app.domain.repository.AppearanceSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppearanceSettingsRepositoryImpl @Inject constructor(
    @AppearanceSettingsDataStore private val dataStore: DataStore<Preferences>
) : AppearanceSettingsRepository {

    override fun observe(): Flow<AppearanceSettings> = dataStore.data.map { it.toAppearanceSettings() }

    override suspend fun update(settings: AppearanceSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.TEXT_SCALE] = settings.textScale.name
            prefs[Keys.FOCUS_STYLE] = settings.focusStyle.name
        }
    }

    private fun Preferences.toAppearanceSettings(): AppearanceSettings {
        val defaults = AppearanceSettings()
        return AppearanceSettings(
            textScale = this[Keys.TEXT_SCALE]?.let { runCatching { TextScale.valueOf(it) }.getOrNull() } ?: defaults.textScale,
            focusStyle = this[Keys.FOCUS_STYLE]?.let { runCatching { FocusStyle.valueOf(it) }.getOrNull() } ?: defaults.focusStyle
        )
    }

    private object Keys {
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val FOCUS_STYLE = stringPreferencesKey("focus_style")
    }
}
