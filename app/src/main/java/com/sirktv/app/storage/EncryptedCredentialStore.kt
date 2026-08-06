package com.sirktv.app.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sirktv.app.domain.model.SavedCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedCredentialStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : CredentialStore {

    private val prefs: SharedPreferences by lazy { buildPreferences() }

    private fun buildPreferences(): SharedPreferences {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } else {
            context.getSharedPreferences(LEGACY_PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    override suspend fun save(credentials: SavedCredentials) = withContext(Dispatchers.IO) {
        val payload = json.encodeToString(SavedCredentials.serializer(), credentials)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            prefs.edit { putString(KEY_CREDENTIALS, payload) }
        } else {
            val encrypted = LegacyCredentialCipher.encrypt(context, payload)
            prefs.edit {
                putString(KEY_LEGACY_IV, encrypted.iv)
                putString(KEY_LEGACY_WRAPPED_KEY, encrypted.wrappedKey)
                putString(KEY_LEGACY_CIPHERTEXT, encrypted.ciphertext)
            }
        }
    }

    override suspend fun get(): SavedCredentials? = withContext(Dispatchers.IO) {
        runCatching {
            val payload = readPayload() ?: return@runCatching null
            json.decodeFromString(SavedCredentials.serializer(), payload)
        }.getOrNull()
    }

    private fun readPayload(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            prefs.getString(KEY_CREDENTIALS, null)
        } else {
            val iv = prefs.getString(KEY_LEGACY_IV, null) ?: return null
            val wrappedKey = prefs.getString(KEY_LEGACY_WRAPPED_KEY, null) ?: return null
            val ciphertext = prefs.getString(KEY_LEGACY_CIPHERTEXT, null) ?: return null
            LegacyCredentialCipher.decrypt(context, LegacyEncryptedPayload(iv, wrappedKey, ciphertext))
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit { clear() }
    }

    private companion object {
        const val PREFS_FILE_NAME = "sirktv_secure_credentials"
        const val LEGACY_PREFS_FILE_NAME = "sirktv_legacy_credentials"
        const val KEY_CREDENTIALS = "credentials"
        const val KEY_LEGACY_IV = "iv"
        const val KEY_LEGACY_WRAPPED_KEY = "wrapped_key"
        const val KEY_LEGACY_CIPHERTEXT = "ciphertext"
    }
}
