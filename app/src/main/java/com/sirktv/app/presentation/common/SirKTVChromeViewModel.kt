package com.sirktv.app.presentation.common

import androidx.lifecycle.ViewModel
import com.sirktv.app.domain.session.CurrentSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Backs [SirKTVChrome] only — sources the greeting name so every screen doesn't need its own plumbing for it. */
@HiltViewModel
class SirKTVChromeViewModel @Inject constructor(
    currentSession: CurrentSession
) : ViewModel() {
    val greetingName: StateFlow<String?> = currentSession.greetingName
}
