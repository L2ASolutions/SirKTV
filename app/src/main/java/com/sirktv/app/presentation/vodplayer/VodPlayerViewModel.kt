package com.sirktv.app.presentation.vodplayer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.ui.PlayerView
import com.sirktv.app.di.ApplicationScope
import com.sirktv.app.domain.model.ContentIds
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.Episode
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.SeriesInfo
import com.sirktv.app.domain.model.StreamQuality
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.usecase.GetPlayerSettingsUseCase
import com.sirktv.app.domain.usecase.GetSeriesInfoUseCase
import com.sirktv.app.domain.usecase.GetSubtitleAppearanceUseCase
import com.sirktv.app.domain.usecase.GetWatchProgressUseCase
import com.sirktv.app.domain.usecase.ObserveMoviesUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesUseCase
import com.sirktv.app.domain.usecase.SaveWatchProgressUseCase
import com.sirktv.app.domain.usecase.ToggleMovieFavoriteUseCase
import com.sirktv.app.domain.usecase.ToggleSeriesFavoriteUseCase
import com.sirktv.app.network.XtreamStreamUrlBuilder
import com.sirktv.app.player.PlaybackState
import com.sirktv.app.player.SirKTVPlayerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class VodPlayerMode { WATCHING, OVERLAY }

data class VodPlayerUiState(
    val mode: VodPlayerMode = VodPlayerMode.WATCHING,
    val isEpisode: Boolean = false,
    val title: String = "",
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val isFavorite: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.Idle,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNextEpisode: Boolean = false,
    val preferredQuality: StreamQuality = StreamQuality.AUTO
)

/**
 * Shared by Movie and Episode playback — both are on-demand content with the
 * same chrome (seek bar, resume, subtitle/audio/quality selection via the
 * same sheets Live TV uses). Which one this instance is showing is decided
 * once, at construction, by which nav args are present: a route with
 * `seriesId` is an episode, a route with only `movieId` is a movie.
 */
@HiltViewModel
class VodPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeMoviesUseCase: ObserveMoviesUseCase,
    private val observeSeriesUseCase: ObserveSeriesUseCase,
    private val getSeriesInfoUseCase: GetSeriesInfoUseCase,
    private val getWatchProgressUseCase: GetWatchProgressUseCase,
    private val saveWatchProgressUseCase: SaveWatchProgressUseCase,
    private val toggleMovieFavoriteUseCase: ToggleMovieFavoriteUseCase,
    private val toggleSeriesFavoriteUseCase: ToggleSeriesFavoriteUseCase,
    private val getPlayerSettingsUseCase: GetPlayerSettingsUseCase,
    private val getSubtitleAppearanceUseCase: GetSubtitleAppearanceUseCase,
    private val currentSession: CurrentSession,
    private val playerEngine: SirKTVPlayerEngine,
    @ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {

    private val movieIdArg: String? = savedStateHandle["movieId"]
    private val seriesId: String? = savedStateHandle["seriesId"]
    private var seasonNumber: Int? = savedStateHandle.get<Int>("season")
    private var episodeNumber: Int? = savedStateHandle.get<Int>("episode")
    private val isEpisode = seriesId != null

    private val _uiState = MutableStateFlow(VodPlayerUiState(isEpisode = isEpisode))
    val uiState: StateFlow<VodPlayerUiState> = _uiState.asStateFlow()

    val tracks: StateFlow<Tracks?> = playerEngine.tracks

    private var seriesInfo: SeriesInfo? = null
    private var hasStartedPlayback = false
    private var progressPollJob: Job? = null
    private var overlayHideJob: Job? = null
    private var tickCount = 0

    // Tracks whichever contentId is currently playing (movieId, or the
    // composite episode id) so onCleared() can persist the final resume
    // position without needing to re-derive it from nav args.
    private var activeContentId: String? = null

    init {
        viewModelScope.launch {
            getPlayerSettingsUseCase().collect { settings ->
                playerEngine.updateSettings(settings)
                _uiState.update { it.copy(preferredQuality = settings.preferredQuality) }
            }
        }
        viewModelScope.launch {
            playerEngine.state.collect { state -> _uiState.update { it.copy(playbackState = state) } }
        }
        viewModelScope.launch {
            getSubtitleAppearanceUseCase().collect { playerEngine.updateSubtitleAppearance(it) }
        }
        if (isEpisode) loadEpisode() else loadMovie()
    }

    private fun loadMovie() {
        val movieId = movieIdArg ?: return
        viewModelScope.launch {
            observeMoviesUseCase().collect { movies ->
                val movie = movies.find { it.id == movieId } ?: return@collect
                _uiState.update { it.copy(title = movie.title, posterUrl = movie.posterUrl, isFavorite = movie.isFavorite) }
                if (!hasStartedPlayback) {
                    hasStartedPlayback = true
                    playMovie(movie)
                }
            }
        }
    }

    private suspend fun playMovie(movie: Movie) {
        val credentials = currentSession.credentials.value ?: return
        val url = XtreamStreamUrlBuilder.buildMovieUrl(
            credentials.serverUrl, credentials.username, credentials.password, movie.id, movie.containerExtension
        )
        val resume = getWatchProgressUseCase(movie.id)?.positionMs ?: 0L
        playerEngine.play(channelId = movie.id, primaryStreamUrl = url, startPositionMs = resume, isLive = false)
        startProgressPoll(movie.id)
    }

    private fun loadEpisode() {
        val sId = seriesId ?: return
        val season = seasonNumber ?: return
        val epNum = episodeNumber ?: return

        viewModelScope.launch {
            observeSeriesUseCase().collect { list ->
                val series = list.find { it.id == sId } ?: return@collect
                _uiState.update { it.copy(title = series.title, posterUrl = series.posterUrl, isFavorite = series.isFavorite) }
            }
        }

        viewModelScope.launch {
            val info = getSeriesInfoUseCase(sId)
            seriesInfo = info
            val episode = info?.episode(season, epNum) ?: return@launch
            _uiState.update {
                it.copy(
                    subtitle = "Season $season · Episode $epNum — ${episode.title}",
                    hasNextEpisode = info.nextEpisode(season, epNum) != null
                )
            }
            if (!hasStartedPlayback) {
                hasStartedPlayback = true
                playEpisode(sId, episode)
            }
        }
    }

    private suspend fun playEpisode(sId: String, episode: Episode) {
        val credentials = currentSession.credentials.value ?: return
        val url = XtreamStreamUrlBuilder.buildEpisodeUrl(
            credentials.serverUrl, credentials.username, credentials.password, episode.id, episode.containerExtension
        )
        val contentId = ContentIds.forEpisode(sId, episode.seasonNumber, episode.episodeNumber)
        val resume = getWatchProgressUseCase(contentId)?.positionMs ?: 0L
        playerEngine.play(channelId = contentId, primaryStreamUrl = url, startPositionMs = resume, isLive = false)
        startProgressPoll(contentId)
    }

    fun onNextEpisode() {
        val sId = seriesId ?: return
        val season = seasonNumber ?: return
        val epNum = episodeNumber ?: return
        val next = seriesInfo?.nextEpisode(season, epNum) ?: return
        seasonNumber = next.seasonNumber
        episodeNumber = next.episodeNumber
        _uiState.update {
            it.copy(
                subtitle = "Season ${next.seasonNumber} · Episode ${next.episodeNumber} — ${next.title}",
                hasNextEpisode = seriesInfo?.nextEpisode(next.seasonNumber, next.episodeNumber) != null
            )
        }
        viewModelScope.launch { playEpisode(sId, next) }
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            if (isEpisode) seriesId?.let { toggleSeriesFavoriteUseCase(it) } else movieIdArg?.let { toggleMovieFavoriteUseCase(it) }
        }
    }

    fun attach(playerView: PlayerView) = playerEngine.attachTo(playerView)

    fun onOkPressed() {
        when (_uiState.value.mode) {
            VodPlayerMode.WATCHING -> setMode(VodPlayerMode.OVERLAY)
            VodPlayerMode.OVERLAY -> playerEngine.togglePlayPause()
        }
    }

    /** First Back press while controls are hidden: reveal them instead of leaving the screen. */
    fun onShowControls() = setMode(VodPlayerMode.OVERLAY)

    fun onSeekBack() = seekBy(-10_000)
    fun onSeekForward() = seekBy(10_000)

    fun onManualRetry() = playerEngine.manualRetry()

    fun selectTrack(group: Tracks.Group, trackIndexInGroup: Int) =
        playerEngine.setTrackOverride(TrackSelectionOverride(group.mediaTrackGroup, trackIndexInGroup))

    fun clearAudioOverride() = playerEngine.clearTrackTypeOverride(C.TRACK_TYPE_AUDIO)
    fun disableSubtitles() = playerEngine.clearTrackTypeOverride(C.TRACK_TYPE_TEXT)

    /** Session-only override — does not touch the persisted Settings default. */
    fun selectQuality(quality: StreamQuality) {
        playerEngine.applyPreferredQuality(quality)
        _uiState.update { it.copy(preferredQuality = quality) }
    }

    private fun seekBy(deltaMs: Long) {
        val target = (_uiState.value.positionMs + deltaMs).coerceAtLeast(0L)
        playerEngine.seekTo(target)
        _uiState.update { it.copy(positionMs = target) }
    }

    private fun setMode(mode: VodPlayerMode) {
        overlayHideJob?.cancel()
        _uiState.update { it.copy(mode = mode) }
        if (mode == VodPlayerMode.OVERLAY) {
            overlayHideJob = viewModelScope.launch {
                delay(5_000)
                if (_uiState.value.mode == VodPlayerMode.OVERLAY) setMode(VodPlayerMode.WATCHING)
            }
        }
    }

    /**
     * Polls the engine every second for the seek bar (it has no position
     * StateFlow of its own — same constraint as the buffer-health reading)
     * and persists watch progress every fifth tick, so a drop mid-movie
     * loses at most a few seconds of resume accuracy.
     */
    private fun startProgressPoll(contentId: String) {
        activeContentId = contentId
        progressPollJob?.cancel()
        tickCount = 0
        progressPollJob = viewModelScope.launch(Dispatchers.Main) {
            while (true) {
                delay(1_000)
                val (position, duration) = playerEngine.snapshotPosition()
                _uiState.update { it.copy(positionMs = position, durationMs = duration) }
                tickCount++
                if (tickCount % 5 == 0 && duration > 0) {
                    persistProgress(contentId, position, duration)
                }
            }
        }
    }

    private suspend fun persistProgress(contentId: String, positionMs: Long, durationMs: Long) {
        val state = _uiState.value
        saveWatchProgressUseCase(
            WatchProgress(
                contentId = contentId,
                contentType = if (isEpisode) ContentType.EPISODE else ContentType.MOVIE,
                title = state.title,
                subtitle = state.subtitle,
                imageUrl = state.posterUrl,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAtEpochMillis = System.currentTimeMillis()
            )
        )
    }

    // Unlike Live TV, VOD/episode playback must NOT survive navigating away —
    // leaving this screen (Back, or any other nav away from the player) has
    // to pause immediately and fully release the ExoPlayer instance so audio
    // doesn't keep running in the background. viewModelScope is already
    // cancelled by the time onCleared() runs, so the final progress save and
    // release are dispatched on the process-lifetime appScope instead.
    override fun onCleared() {
        progressPollJob?.cancel()
        overlayHideJob?.cancel()
        playerEngine.pause()
        val (position, duration) = playerEngine.snapshotPosition()
        val contentId = activeContentId
        appScope.launch {
            if (contentId != null && duration > 0) {
                persistProgress(contentId, position, duration)
            }
            playerEngine.release()
        }
    }
}
