package com.sirktv.app.presentation.home

import androidx.lifecycle.ViewModel
import com.sirktv.app.domain.session.CurrentSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Home is now a pure 5-tile launcher (Live TV/Movies/Series/Favorites/
 * Settings) — no content lists, no personal rows, nothing that reads Room or
 * calls the Xtream API. The only state it needs is the greeting name.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    currentSession: CurrentSession
) : ViewModel() {

    val displayName: StateFlow<String?> = currentSession.greetingName
}
