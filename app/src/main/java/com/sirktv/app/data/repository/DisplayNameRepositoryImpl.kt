package com.sirktv.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sirktv.app.di.DisplayNameDataStore
import com.sirktv.app.domain.repository.DisplayNameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DisplayNameRepositoryImpl @Inject constructor(
    @DisplayNameDataStore private val dataStore: DataStore<Preferences>
) : DisplayNameRepository {

    override fun observe(): Flow<String?> = dataStore.data.map { it[Keys.DISPLAY_NAME] }

    override suspend fun get(): String? = dataStore.data.first()[Keys.DISPLAY_NAME]

    override suspend fun set(displayName: String?) {
        dataStore.edit { prefs ->
            if (displayName.isNullOrBlank()) prefs.remove(Keys.DISPLAY_NAME) else prefs[Keys.DISPLAY_NAME] = displayName
        }
    }

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }
}
