package com.sirktv.app.domain.session

import com.sirktv.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache of the authenticated profile, shared across screens for the
 * lifetime of the process. Not a substitute for [com.sirktv.app.storage.CredentialStore],
 * which is what survives process death.
 */
@Singleton
class CurrentSession @Inject constructor() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    fun set(profile: UserProfile) {
        _profile.value = profile
    }

    fun clear() {
        _profile.value = null
    }
}
