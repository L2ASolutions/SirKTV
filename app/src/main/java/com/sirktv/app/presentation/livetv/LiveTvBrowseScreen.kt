package com.sirktv.app.presentation.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.presentation.common.SirKTVChrome
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceStrong
import com.sirktv.app.presentation.theme.SirKTVPrimary

private val SidebarWidth = 200.dp
internal val ChannelListWidth = 340.dp

/**
 * Landing point for Live TV: country sidebar, channel list, and a preview
 * panel so the user picks a channel manually. Nothing plays until "Watch
 * Full Screen" is pressed — this screen never auto-tunes on its own.
 */
@Composable
fun LiveTvBrowseScreen(
    onChannelSelected: (channelId: String) -> Unit,
    onNavigate: (SirKTVNavItem) -> Unit,
    viewModel: LiveTvBrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().background(SirKTVBackground)) {
        Box(Modifier.padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)) {
            SirKTVChrome(activeItem = SirKTVNavItem.LIVE_TV, onNavigate = onNavigate, onRefresh = viewModel::refresh)
        }

        when {
            uiState.isLoading -> LoadingChannelsState()
            uiState.loadError != null && uiState.allChannels.isEmpty() -> LoadErrorState(
                message = uiState.loadError!!,
                onRetry = viewModel::refresh
            )
            else -> Row(Modifier.fillMaxSize()) {
                CountrySidebar(
                    countryGroups = uiState.countryGroups,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onAllCountries = { viewModel.onCategorySelected(null) },
                    onCategorySelected = viewModel::onCategorySelected
                )
                ChannelListColumn(
                    channels = uiState.visibleChannels,
                    categoryName = { id -> uiState.categories.find { it.id == id }?.name },
                    countryLabel = { id -> uiState.countryGroups.find { group -> group.categories.any { it.id == id } }?.let { "${it.code} ${it.name}" } },
                    selectedChannelId = uiState.selectedChannel?.id,
                    epgCache = uiState.epgCache,
                    onVisible = viewModel::requestEpgFor,
                    onHighlight = viewModel::onChannelHighlighted,
                    onToggleFavorite = viewModel::onToggleFavorite
                )
                PreviewPanel(
                    channel = uiState.selectedChannel,
                    nowNext = uiState.selectedChannel?.let { uiState.epgCache[it.id] },
                    listings = uiState.selectedChannel?.let { uiState.epgListingsCache[it.id] }.orEmpty(),
                    onRequestListings = viewModel::requestEpgListingsFor,
                    onWatchFullScreen = { channel -> onChannelSelected(channel.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LoadingChannelsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            SirKTVLogoMark()
            CircularProgressIndicator(color = SirKTVPrimary)
            Text("Loading channels…", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "Fetching categories and channels from your provider",
                color = SirKTVOnSurfaceMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LoadErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            Text("No channels available", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(message, color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
            Button(onClick = onRetry, modifier = Modifier.padding(top = Dimens.SpaceSm).tvFocusStyle()) {
                TvText("Retry")
            }
        }
    }
}

@Composable
private fun CountrySidebar(
    countryGroups: List<CountryGroup>,
    selectedCategoryId: String?,
    onAllCountries: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    var expandedCode by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxHeight().width(SidebarWidth).background(Color(0xEE0A0A0F))) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(vertical = Dimens.SpaceMd, horizontal = Dimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                Text(
                    text = "COUNTRIES",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp, start = 8.dp)
                )
            }
            item {
                SidebarRow(label = "All Countries", selected = selectedCategoryId == null, onClick = onAllCountries)
            }
            items(countryGroups, key = { it.code }) { group ->
                val isExpanded = expandedCode == group.code
                SidebarRow(
                    label = "${group.code} · ${group.name}",
                    selected = false,
                    onClick = { expandedCode = if (isExpanded) null else group.code }
                )
                if (isExpanded) {
                    group.categories.forEach { category ->
                        SidebarRow(
                            label = category.name,
                            selected = selectedCategoryId == category.id,
                            indent = true,
                            onClick = { onCategorySelected(category.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarRow(label: String, selected: Boolean, indent: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) SirKTVPrimary.copy(alpha = 0.22f) else Color.Transparent, RoundedCornerShape(8.dp))
                .padding(start = if (indent) 20.dp else 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Reused by [LiveTvBrowseScreen] (blue accent) and SportsScreen (green accent). */
@Composable
internal fun ChannelListColumn(
    channels: List<Channel>,
    categoryName: (String) -> String?,
    countryLabel: (String) -> String?,
    selectedChannelId: String?,
    epgCache: Map<String, EpgNowNext>,
    onVisible: (String) -> Unit,
    onHighlight: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    accentColor: Color = SirKTVPrimary
) {
    Box(Modifier.fillMaxHeight().width(ChannelListWidth)) {
        if (channels.isEmpty()) {
            Text(
                "No channels in this category yet.",
                color = SirKTVOnSurfaceMuted,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center).padding(Dimens.SpaceMd)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(channels, key = { it.id }) { channel ->
                    LaunchedEffect(channel.id) { onVisible(channel.id) }
                    BrowseChannelRow(
                        channel = channel,
                        nowNext = epgCache[channel.id],
                        categoryLabel = categoryName(channel.categoryId),
                        countryLabel = countryLabel(channel.categoryId),
                        isSelected = channel.id == selectedChannelId,
                        onClick = { onHighlight(channel.id) },
                        onToggleFavorite = { onToggleFavorite(channel.id) },
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseChannelRow(
    channel: Channel,
    nowNext: EpgNowNext?,
    categoryLabel: String?,
    countryLabel: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    accentColor: Color = SirKTVPrimary
) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SirKTVCardBackground, RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(if (isSelected) accentColor else Color.Transparent, RoundedCornerShape(2.dp))
            )
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.85f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(channel.name.take(2).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    text = channel.name,
                    color = SirKTVOnSurfaceStrong,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = nowNext?.now?.title ?: "Schedule unavailable",
                    color = SirKTVOnSurfaceMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 3.dp)) {
                    countryLabel?.let { InlineTag(it) }
                    categoryLabel?.let { InlineTag(it) }
                }
            }
            FavoriteHeartButton(isFavorite = channel.isFavorite, onToggle = onToggleFavorite, modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
private fun InlineTag(text: String) {
    Box(Modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(text, color = SirKTVOnSurfaceMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Reused by [LiveTvBrowseScreen] (blue accent) and SportsScreen (green accent). */
@Composable
internal fun PreviewPanel(
    channel: Channel?,
    nowNext: EpgNowNext?,
    listings: List<EpgProgram>,
    onRequestListings: (String) -> Unit,
    onWatchFullScreen: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = SirKTVPrimary
) {
    Box(modifier.fillMaxHeight()) {
        if (channel == null) {
            Text(
                "Select a channel to preview it here.",
                color = SirKTVOnSurfaceMuted,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        LaunchedEffect(channel.id) { onRequestListings(channel.id) }

        Column(
            modifier = Modifier.fillMaxSize().padding(Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Brush.linearGradient(listOf(accentColor.copy(alpha = 0.35f), Color.Black)), RoundedCornerShape(Dimens.CornerRadius))
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color(0xFFFF4757), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.align(Alignment.Center).size(56.dp).background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Color.White, fontSize = 22.sp)
                }
            }

            Text(channel.name, color = SirKTVOnSurfaceStrong, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            nowNext?.now?.let { now ->
                Text(now.title, color = SirKTVOnSurfaceMuted, fontSize = 13.sp)
                ScheduleProgressBar(now, accentColor)
            }

            Button(
                onClick = { onWatchFullScreen(channel) },
                colors = androidx.tv.material3.ButtonDefaults.colors(containerColor = accentColor),
                modifier = Modifier.fillMaxWidth().tvFocusStyle()
            ) {
                TvText("▶ Watch Full Screen")
            }

            Text("Schedule", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = Dimens.SpaceSm))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listings.take(6), key = { it.startEpochSeconds }) { program ->
                    ScheduleRow(program = program, isNow = program == nowNext?.now, isNext = program == nowNext?.next, accentColor = accentColor)
                }
            }
        }
    }
}

@Composable
private fun ScheduleProgressBar(program: EpgProgram, accentColor: Color) {
    val total = (program.endEpochSeconds - program.startEpochSeconds).toFloat()
    val fraction = if (total > 0) {
        val nowSeconds = System.currentTimeMillis() / 1000
        ((nowSeconds - program.startEpochSeconds) / total).coerceIn(0f, 1f)
    } else 0f
    Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(999.dp))) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(accentColor, RoundedCornerShape(999.dp)))
    }
}

@Composable
private fun ScheduleRow(program: EpgProgram, isNow: Boolean, isNext: Boolean, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(formatEpochSeconds(program.startEpochSeconds), color = SirKTVOnSurfaceMuted, fontSize = 11.sp, modifier = Modifier.width(48.dp))
        Text(
            text = program.title,
            color = SirKTVOnSurfaceStrong,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        when {
            isNow -> Tag(text = "NOW", color = accentColor)
            isNext -> Tag(text = "NEXT", color = SirKTVOnSurfaceMuted)
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Box(Modifier.background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
