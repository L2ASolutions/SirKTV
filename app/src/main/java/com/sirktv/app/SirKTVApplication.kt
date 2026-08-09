package com.sirktv.app

import android.app.Application
import com.sirktv.app.presentation.player.MediaSessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SirKTVApplication : Application() {

    @Inject
    lateinit var mediaSessionManager: MediaSessionManager

    // Only fires on emulators/rare low-memory teardowns per the platform docs, but it's
    // the correct last-resort place to release the process-lifetime MediaSessionCompat —
    // normal exits already go through MediaSessionManager.exitPlayerScreen().
    override fun onTerminate() {
        super.onTerminate()
        mediaSessionManager.release()
    }
}
