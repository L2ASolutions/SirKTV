package com.sirktv.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sirktv.app.di.SubtitleAppearanceDataStore
import com.sirktv.app.domain.model.SubtitleAppearance
import com.sirktv.app.domain.model.SubtitleBackground
import com.sirktv.app.domain.model.SubtitleEdgeStyle
import com.sirktv.app.domain.model.SubtitleTextColor
import com.sirktv.app.domain.model.SubtitleTextSize
import com.sirktv.app.domain.repository.SubtitleAppearanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SubtitleAppearanceRepositoryImpl @Inject constructor(
    @SubtitleAppearanceDataStore private val dataStore: DataStore<Preferences>
) : SubtitleAppearanceRepository {

    override fun observe(): Flow<SubtitleAppearance> = dataStore.data.map { it.toSubtitleAppearance() }

    override suspend fun update(settings: SubtitleAppearance) {
        dataStore.edit { prefs ->
            prefs[Keys.TEXT_SIZE] = settings.textSize.name
            prefs[Keys.TEXT_COLOR] = settings.textColor.name
            prefs[Keys.BACKGROUND] = settings.background.name
            prefs[Keys.EDGE_STYLE] = settings.edgeStyle.name
        }
    }

    private fun Preferences.toSubtitleAppearance(): SubtitleAppearance {
        val defaults = SubtitleAppearance()
        return SubtitleAppearance(
            textSize = this[Keys.TEXT_SIZE]?.let { runCatching { SubtitleTextSize.valueOf(it) }.getOrNull() } ?: defaults.textSize,
            textColor = this[Keys.TEXT_COLOR]?.let { runCatching { SubtitleTextColor.valueOf(it) }.getOrNull() } ?: defaults.textColor,
            background = this[Keys.BACKGROUND]?.let { runCatching { SubtitleBackground.valueOf(it) }.getOrNull() } ?: defaults.background,
            edgeStyle = this[Keys.EDGE_STYLE]?.let { runCatching { SubtitleEdgeStyle.valueOf(it) }.getOrNull() } ?: defaults.edgeStyle
        )
    }

    private object Keys {
        val TEXT_SIZE = stringPreferencesKey("subtitle_text_size")
        val TEXT_COLOR = stringPreferencesKey("subtitle_text_color")
        val BACKGROUND = stringPreferencesKey("subtitle_background")
        val EDGE_STYLE = stringPreferencesKey("subtitle_edge_style")
    }
}
