package com.sirktv.app.presentation.sports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVSportsAccent
import com.sirktv.app.presentation.theme.SirKTVSportsAccentBright
import androidx.tv.material3.Surface

@Composable
fun SportsScreen(
    onChannelSelected: (channelId: String) -> Unit,
    viewModel: SportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundBrush = Brush.radialGradient(
        colors = listOf(SirKTVSportsAccent.copy(alpha = 0.18f), SirKTVBackground),
        center = Offset(0.85f, 0.05f),
        radius = 1400f
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical)
    ) {
        Text("Sports", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SirKTVSportsAccent)
        Text(
            text = "${uiState.allChannels.size} sports channel(s) on this account",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = Dimens.SpaceMd)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                CategoryPill(label = "All Live", selected = uiState.selectedCategoryId == null) {
                    viewModel.onCategorySelected(null)
                }
            }
            items(uiState.categories, key = { it.id }) { category ->
                CategoryPill(label = category.name, selected = uiState.selectedCategoryId == category.id) {
                    viewModel.onCategorySelected(category.id)
                }
            }
        }

        Box(Modifier.padding(top = Dimens.SpaceMd)) {
            if (uiState.visibleChannels.isEmpty()) {
                Text(
                    "No sports channels detected on this account yet.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.visibleChannels, key = { it.id }) { channel ->
                        SportsChannelRow(
                            channel = channel,
                            eventTitle = uiState.epgCache[channel.id]?.now?.title,
                            onSelect = { onChannelSelected(channel.id) },
                            onVisible = { viewModel.requestEpgFor(channel.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.tvFocusStyle(cornerRadius = 999.dp)) {
        Box(
            modifier = Modifier
                .background(if (selected) SirKTVSportsAccent else Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = if (selected) Color(0xFF04140A) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SportsChannelRow(
    channel: Channel,
    eventTitle: String?,
    onSelect: () -> Unit,
    onVisible: () -> Unit
) {
    LaunchedEffect(channel.id) { onVisible() }

    Surface(onClick = onSelect, modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(30.dp).background(SirKTVSportsAccent.copy(alpha = 0.25f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("●", color = SirKTVSportsAccentBright, fontSize = 12.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${channel.channelNumber} · ${channel.name}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = eventTitle ?: "Schedule unavailable",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text("LIVE", color = SirKTVSportsAccentBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
