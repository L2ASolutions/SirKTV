package com.sirktv.app

import android.content.ComponentCallbacks2
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sirktv.app.player.SirKTVPlayerEngine
import com.sirktv.app.presentation.navigation.SirKTVNavHost
import com.sirktv.app.presentation.screensaver.IdleScreensaverHost
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerEngine: SirKTVPlayerEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SirKTVTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = SirKTVBackground) {
                    IdleScreensaverHost {
                        SirKTVNavHost()
                    }
                }
            }
        }
    }

    // Pause (not release) on ordinary backgrounding: the PlayerView only ever
    // attaches to the engine once, in its AndroidView factory — releasing the
    // ExoPlayer here would leave that view pointing at a dead instance with no
    // way to re-attach short of navigating away and back. Pausing keeps the
    // same instance alive so returning to the foreground just resumes it.
    // While in PiP, playback should keep running, so onStop is a no-op there.
    override fun onStop() {
        super.onStop()
        if (!isInPipModeCompat()) {
            playerEngine.pause()
        }
    }

    override fun onStart() {
        super.onStart()
        playerEngine.resume()
    }

    // A real memory-pressure signal is the one case where a full release is
    // worth the cost of losing the live instance — the process is likely to be
    // killed soon regardless, and relaunching goes through the normal
    // auto-start flow again.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            playerEngine.release()
        }
    }

    private fun isInPipModeCompat(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode
}
