package com.sirktv.app

import android.app.SearchManager
import android.content.ComponentCallbacks2
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sirktv.app.player.SirKTVPlayerEngine
import com.sirktv.app.presentation.navigation.NavigationCommand
import com.sirktv.app.presentation.navigation.NavigationCommandBus
import com.sirktv.app.presentation.navigation.SirKTVNavHost
import com.sirktv.app.presentation.player.ActivePlayerState
import com.sirktv.app.presentation.screensaver.IdleScreensaverHost
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerEngine: SirKTVPlayerEngine

    @Inject
    lateinit var activePlayerState: ActivePlayerState

    @Inject
    lateinit var navigationCommandBus: NavigationCommandBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashRecoveryHandler()
        handleSearchIntent(intent)
        setContent {
            SirKTVTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = SirKTVBackground) {
                    IdleScreensaverHost {
                        SirKTVNavHost(navigationCommandBus = navigationCommandBus)
                    }
                }
            }
        }
    }

    // The Fire TV remote's mic/voice button fires ACTION_SEARCH (with the
    // recognized query in EXTRA_QUERY) as a new intent against this
    // singleTop activity rather than a fresh launch, so it lands here instead
    // of onCreate on every subsequent press.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSearchIntent(intent)
    }

    private fun handleSearchIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY).orEmpty()
            navigationCommandBus.send(NavigationCommand.OpenSearch(query))
        }
    }

    // Compose can miss hardware key events entirely when the overlay is
    // hidden or nothing in the tree currently holds focus, so every Fire TV
    // remote keycode that must always work is caught here first and routed
    // through the single active player handle (if any) rather than
    // duplicated against ExoPlayer/navigation directly.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val handle = activePlayerState.current
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { handle?.togglePlayPause(); true }
            KeyEvent.KEYCODE_MEDIA_PLAY -> { handle?.play(); true }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> { handle?.pause(); true }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { handle?.fastForward(); true }
            KeyEvent.KEYCODE_MEDIA_REWIND -> { handle?.rewind(); true }
            KeyEvent.KEYCODE_MEDIA_NEXT -> { handle?.skipToNext(); true }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { handle?.skipToPrevious(); true }
            KeyEvent.KEYCODE_SEARCH -> {
                navigationCommandBus.send(NavigationCommand.OpenSearch())
                true
            }
            KeyEvent.KEYCODE_MENU -> {
                if (handle != null) handle.showQuickActions() else navigationCommandBus.send(NavigationCommand.MenuPressed)
                true
            }
            else -> super.onKeyDown(keyCode, event)
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

    // A last-resort net for whatever runCatching/try-catch missed elsewhere in
    // the app: by the time the JVM hands us a truly uncaught exception, this
    // thread is already unwinding, so there's no surviving Compose tree to
    // navigate within — the only reliable recovery is a fresh restart. Landing
    // back on this same Activity re-enters SirKTVNavHost at Login, which
    // silently re-authenticates a saved session and routes to Home in the
    // same couple of frames a normal cold start would, so this reads to the
    // user as "back to Home" rather than a crash.
    private fun installCrashRecoveryHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                Log.e("SIRKTV_FATAL", "=== CRASH ===")
                Log.e("SIRKTV_FATAL", throwable.stackTraceToString())
                Log.e("SIRKTV_FATAL", "=== END CRASH ===")
                Log.e("SirKTV_Crash", "Uncaught exception, recovering to Home", throwable)
                val restartIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(restartIntent)
            } catch (recoveryError: Throwable) {
                Log.e("SirKTV_Crash", "Crash recovery itself failed", recoveryError)
            } finally {
                Process.killProcess(Process.myPid())
            }
        }
    }
}
