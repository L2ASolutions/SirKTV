package com.sirktv.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.presentation.common.SirKTVChrome
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.livetv.ChannelCard
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground

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
                            badge = "NEW",
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
