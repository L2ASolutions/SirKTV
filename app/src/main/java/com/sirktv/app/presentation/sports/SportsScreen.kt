package com.sirktv.app.presentation.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Surface
import com.sirktv.app.presentation.common.SirKTVChrome
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.livetv.ChannelListColumn
import com.sirktv.app.presentation.livetv.PreviewPanel
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVSportsAccent

private val SidebarWidth = 200.dp

/** Same three-column layout as Live TV Browse, but with a flat sport-type filter and the green sports accent throughout. */
@Composable
fun SportsScreen(
    onChannelSelected: (channelId: String) -> Unit,
    onNavigate: (SirKTVNavItem) -> Unit,
    viewModel: SportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize().background(SirKTVBackground)) {
        Box(Modifier.padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)) {
            SirKTVChrome(activeItem = SirKTVNavItem.SPORTS, onNavigate = onNavigate, onRefresh = {})
        }

        Row(Modifier.fillMaxSize()) {
            FilterSidebar(selectedFilter = uiState.selectedFilter, onFilterSelected = viewModel::onFilterSelected)
            ChannelListColumn(
                channels = uiState.visibleChannels,
                categoryName = { id -> uiState.categories.find { it.id == id }?.name },
                countryLabel = { null },
                selectedChannelId = uiState.selectedChannel?.id,
                epgCache = uiState.epgCache,
                onVisible = viewModel::requestEpgFor,
                onHighlight = viewModel::onChannelHighlighted,
                onToggleFavorite = viewModel::onToggleFavorite,
                accentColor = SirKTVSportsAccent
            )
            PreviewPanel(
                channel = uiState.selectedChannel,
                nowNext = uiState.selectedChannel?.let { uiState.epgCache[it.id] },
                listings = uiState.selectedChannel?.let { uiState.epgListingsCache[it.id] }.orEmpty(),
                onRequestListings = viewModel::requestEpgListingsFor,
                onWatchFullScreen = { channel -> onChannelSelected(channel.id) },
                modifier = Modifier.weight(1f),
                accentColor = SirKTVSportsAccent
            )
        }
    }
}

@Composable
private fun FilterSidebar(selectedFilter: SportsFilter, onFilterSelected: (SportsFilter) -> Unit) {
    Box(Modifier.fillMaxHeight().width(SidebarWidth).background(Color(0xEE0A0A0F))) {
        LazyColumn(
            modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(vertical = Dimens.SpaceMd, horizontal = Dimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                Text(
                    text = "FILTER",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp, start = 8.dp)
                )
            }
            items(SportsFilter.entries.toList(), key = { it.name }) { filter ->
                val selected = filter == selectedFilter
                Surface(onClick = { onFilterSelected(filter) }, modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (selected) SirKTVSportsAccent.copy(alpha = 0.22f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = filter.label,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
