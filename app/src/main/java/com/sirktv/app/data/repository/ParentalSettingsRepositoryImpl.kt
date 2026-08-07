package com.sirktv.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sirktv.app.di.ParentalSettingsDataStore
import com.sirktv.app.domain.model.ParentalSettings
import com.sirktv.app.domain.repository.ParentalSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject

class ParentalSettingsRepositoryImpl @Inject constructor(
    @ParentalSettingsDataStore private val dataStore: DataStore<Preferences>
) : ParentalSettingsRepository {

    override fun observe(): Flow<ParentalSettings> = dataStore.data.map { it.toParentalSettings() }

    override suspend fun get(): ParentalSettings = dataStore.data.first().toParentalSettings()

    override suspend fun setPin(pin: String?) {
        dataStore.edit { prefs ->
            if (pin.isNullOrBlank()) {
                prefs.remove(Keys.PIN_HASH)
                prefs.remove(Keys.LOCKED_CATEGORIES)
            } else {
                prefs[Keys.PIN_HASH] = hash(pin)
            }
        }
    }

    override suspend fun setCategoryLocked(categoryId: String, locked: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.LOCKED_CATEGORIES] ?: emptySet()
            prefs[Keys.LOCKED_CATEGORIES] = if (locked) current + categoryId else current - categoryId
        }
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val storedHash = dataStore.data.first()[Keys.PIN_HASH] ?: return false
        return storedHash == hash(pin)
    }

    private fun Preferences.toParentalSettings(): ParentalSettings = ParentalSettings(
        pinHash = this[Keys.PIN_HASH],
        lockedCategoryIds = this[Keys.LOCKED_CATEGORIES] ?: emptySet()
    )

    private fun hash(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val LOCKED_CATEGORIES = stringSetPreferencesKey("locked_category_ids")
    }
}
