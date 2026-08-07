package com.sirktv.app.domain.session

import com.sirktv.app.domain.model.SavedCredentials
import com.sirktv.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache of the authenticated profile and credentials, shared across
 * screens for the lifetime of the process. Every Xtream stream URL embeds the
 * raw username/password in its path, so playback needs the password for the
 * whole session even when "remember me" was off and nothing was persisted to
 * [com.sirktv.app.storage.CredentialStore]. Never written to disk from here.
 */
@Singleton
class CurrentSession @Inject constructor() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _credentials = MutableStateFlow<SavedCredentials?>(null)
    val credentials: StateFlow<SavedCredentials?> = _credentials.asStateFlow()

    fun set(profile: UserProfile, credentials: SavedCredentials) {
        _profile.value = profile
        _credentials.value = credentials
    }

    fun clear() {
        _profile.value = null
        _credentials.value = null
    }
}
