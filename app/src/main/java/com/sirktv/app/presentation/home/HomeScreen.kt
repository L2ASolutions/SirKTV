package com.sirktv.app.presentation.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Text as TvText
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.util.RecentlyAdded
import com.sirktv.app.presentation.common.SirKTVChrome
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.livetv.ChannelCard
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVSurface

private val LiveTvCardWidth = 300.dp
private val PosterCardWidth = 140.dp

@Composable
fun HomeScreen(
    onOpenContent: (HomeNavTarget) -> Unit,
    onNavigate: (SirKTVNavItem) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val channelEpgCache by viewModel.channelEpgCache.collectAsState()
    val movieSynopsisCache by viewModel.movieSynopsisCache.collectAsState()
    var previewItem by remember { mutableStateOf<HeroItem?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    // Home is effectively the root of the back stack after login — Back here
    // must never exit immediately, only after the user confirms.
    BackHandler(enabled = !showExitDialog) { showExitDialog = true }
    BackHandler(enabled = showExitDialog) { showExitDialog = false }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.NavigateToLogin -> onLoggedOut()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(SirKTVBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = Dimens.SafeAreaHorizontal,
                vertical = Dimens.SafeAreaVertical
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)
        ) {
            item {
                SirKTVChrome(activeItem = SirKTVNavItem.HOME, onNavigate = onNavigate, onRefresh = viewModel::refresh)
            }

            item {
                HeroCarousel(items = state.heroItems, onItemClick = { previewItem = it })
            }

            if (state.continueWatching.isNotEmpty()) {
                item {
                    MediaRow(title = "Continue Watching", rowItems = state.continueWatching) { progress ->
                        MediaCard(
                            title = progress.title,
                            imageUrl = progress.imageUrl,
                            subtitle = progress.subtitle,
                            progressFraction = progress.progressFraction,
                            onClick = { viewModel.watchProgressTarget(progress)?.let(onOpenContent) },
                            modifier = Modifier.width(LiveTvCardWidth)
                        )
                    }
                }
            }

            if (state.recentlyAdded.isNotEmpty()) {
                item {
                    MediaRow(title = "Recently Added", rowItems = state.recentlyAdded) { added ->
                        MediaCard(
                            title = added.title,
                            imageUrl = added.imageUrl,
                            aspectRatio = 2f / 3f,
                            rating = added.rating,
                            badge = if (RecentlyAdded.isWithinNewWindow(added.addedAtEpochMillis)) "NEW" else null,
                            isFavorite = added.isFavorite,
                            onClick = { onOpenContent(added.navTarget) },
                            modifier = Modifier.width(PosterCardWidth)
                        )
                    }
                }
            }

            if (state.recentlyWatched.isNotEmpty()) {
                item {
                    MediaRow(title = "Recently Watched", rowItems = state.recentlyWatched) { progress ->
                        MediaCard(
                            title = progress.title,
                            imageUrl = progress.imageUrl,
                            aspectRatio = 2f / 3f,
                            onClick = { viewModel.watchProgressTarget(progress)?.let(onOpenContent) },
                            modifier = Modifier.width(PosterCardWidth)
                        )
                    }
                }
            }

            if (state.liveChannels.isNotEmpty()) {
                item {
                    LiveTvRow(
                        channels = state.liveChannels,
                        epgCache = channelEpgCache,
                        onRequestEpg = viewModel::requestEpgFor,
                        onChannelClick = { channelId -> onOpenContent(HomeNavTarget.LiveTv(channelId)) }
                    )
                }
            }
        }

        previewItem?.let { item ->
            LaunchedEffect(item.contentId) {
                if (item.contentType == ContentType.MOVIE) viewModel.requestMovieSynopsis(item.contentId)
            }
            val synopsis = if (item.contentType == ContentType.MOVIE) movieSynopsisCache[item.contentId] else item.synopsis
            HeroPreviewModal(
                item = item,
                synopsis = synopsis,
                onPlay = { onOpenContent(item.navTarget); previewItem = null },
                onToggleFavorite = {
                    when (item.contentType) {
                        ContentType.MOVIE -> viewModel.onToggleMovieFavorite(item.contentId)
                        ContentType.SERIES -> viewModel.onToggleSeriesFavorite(item.contentId)
                        else -> Unit
                    }
                },
                onDismiss = { previewItem = null }
            )
        }

        if (showExitDialog) {
            ExitConfirmationDialog(
                onConfirm = { activity?.finish() },
                onDismiss = { showExitDialog = false }
            )
        }
    }
}

@Composable
private fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val noFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { noFocusRequester.requestFocus() }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .background(SirKTVSurface, RoundedCornerShape(16.dp))
                .padding(Dimens.SpaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
        ) {
            Text("Exit SirKTV?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Button(onClick = onDismiss, modifier = Modifier.focusRequester(noFocusRequester).tvFocusStyle(accent = TvFocusAccent.BORDER)) {
                    TvText("No")
                }
                Button(onClick = onConfirm, modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)) {
                    TvText("Yes")
                }
            }
        }
    }
}

@Composable
private fun LiveTvRow(
    channels: List<Channel>,
    epgCache: Map<String, EpgNowNext>,
    onRequestEpg: (String) -> Unit,
    onChannelClick: (String) -> Unit
) {
    Column {
        RowHeader(title = "Live TV", count = channels.size)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            items(channels, key = { it.id }) { channel ->
                LaunchedEffect(channel.id) { onRequestEpg(channel.id) }
                ChannelCard(
                    channel = channel,
                    nowNext = epgCache[channel.id],
                    onClick = { onChannelClick(channel.id) },
                    modifier = Modifier.width(LiveTvCardWidth)
                )
            }
        }
    }
}
