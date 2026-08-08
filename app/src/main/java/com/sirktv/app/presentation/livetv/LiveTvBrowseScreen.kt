package com.sirktv.app.presentation.livetv

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary

private val SidebarWidth = 260.dp

/**
 * Landing point for Live TV: shows the category sidebar and channel list so
 * the user picks a channel manually. Nothing plays until [onChannelSelected]
 * fires — this screen never auto-tunes on its own.
 */
@Composable
fun LiveTvBrowseScreen(
    onChannelSelected: (channelId: String) -> Unit,
    viewModel: LiveTvBrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(Modifier.fillMaxSize().background(SirKTVBackground)) {
        when {
            uiState.isLoading -> LoadingChannelsState()
            uiState.loadError != null && uiState.allChannels.isEmpty() -> LoadErrorState(
                message = uiState.loadError!!,
                onRetry = viewModel::refresh
            )
            else -> Row(Modifier.fillMaxSize()) {
                CategorySidebar(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelected = viewModel::onCategorySelected
                )
                ChannelGrid(
                    channels = uiState.visibleChannels,
                    epgCache = uiState.epgCache,
                    onChannelSelected = { onChannelSelected(it.id) },
                    onToggleFavorite = viewModel::onToggleFavorite,
                    onVisible = viewModel::requestEpgFor
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
private fun CategorySidebar(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    Box(Modifier.fillMaxHeight().width(SidebarWidth).background(Color(0xEE0A0A0F))) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(vertical = Dimens.SafeAreaVertical, horizontal = Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "LIVE TV",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            item {
                CategoryRow(label = "All Channels", selected = selectedCategoryId == null, onClick = { onCategorySelected(null) })
            }
            items(categories, key = { it.id }) { category ->
                CategoryRow(
                    label = category.name,
                    selected = selectedCategoryId == category.id,
                    onClick = { onCategorySelected(category.id) }
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) SirKTVPrimary.copy(alpha = 0.22f) else Color.Transparent, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChannelGrid(
    channels: List<Channel>,
    epgCache: Map<String, EpgNowNext>,
    onChannelSelected: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onVisible: (String) -> Unit
) {
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        if (channels.isEmpty()) {
            Text(
                "No channels in this category yet.",
                color = SirKTVOnSurfaceMuted,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(channels, key = { it.id }) { channel ->
                    LaunchedEffect(channel.id) { onVisible(channel.id) }
                    ChannelCard(
                        channel = channel,
                        nowNext = epgCache[channel.id],
                        onClick = { onChannelSelected(channel) },
                        onToggleFavorite = {
                            Toast.makeText(
                                context,
                                if (channel.isFavorite) "Removed from Favorites" else "Added to Favorites",
                                Toast.LENGTH_SHORT
                            ).show()
                            onToggleFavorite(channel.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
