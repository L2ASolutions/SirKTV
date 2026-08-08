package com.sirktv.app.domain.session

import com.sirktv.app.di.ApplicationScope
import com.sirktv.app.domain.model.SavedCredentials
import com.sirktv.app.domain.model.UserProfile
import com.sirktv.app.domain.repository.DisplayNameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
class CurrentSession @Inject constructor(
    displayNameRepository: DisplayNameRepository,
    @ApplicationScope appScope: CoroutineScope
) {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _credentials = MutableStateFlow<SavedCredentials?>(null)
    val credentials: StateFlow<SavedCredentials?> = _credentials.asStateFlow()

    /** The persisted "How should we greet you?" name, falling back to the account username when blank/unset. */
    val greetingName: StateFlow<String?> = combine(profile, displayNameRepository.observe()) { profile, displayName ->
        displayName?.takeIf { it.isNotBlank() } ?: profile?.username
    }.stateIn(appScope, SharingStarted.Eagerly, null)

    fun set(profile: UserProfile, credentials: SavedCredentials) {
        _profile.value = profile
        _credentials.value = credentials
    }

    fun clear() {
        _profile.value = null
        _credentials.value = null
    }
}
