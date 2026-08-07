package com.sirktv.app.presentation.livetv

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary
import androidx.tv.material3.Surface

/**
 * List-style program guide (channel -> now/next), reachable via D-pad Right
 * from Live TV. Scoped down from the full timeline-grid in the visual spec —
 * that needs a custom time-positioned Layout and is a follow-up polish pass;
 * this covers the same job ("what's on, across channels, right now").
 */
@Composable
fun ProgramGuidePanel(
    channels: List<Channel>,
    currentChannelId: String?,
    onChannelSelected: (Channel) -> Unit,
    viewModel: LiveTvPlayerViewModel = hiltViewModel()
) {
    val epgCache by viewModel.channelEpgCache.collectAsState()
    val listState = rememberLazyListState()
    val currentIndex = channels.indexOfFirst { it.id == currentChannelId }.takeIf { it >= 0 } ?: 0

    LaunchedEffect(channels) {
        if (currentIndex > 0) listState.scrollToItem(currentIndex)
    }

    Box(Modifier.fillMaxSize().background(Color(0xF205070C))) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text("Program Guide", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(channels, key = { it.id }) { channel ->
                GuideRow(
                    channel = channel,
                    isCurrent = channel.id == currentChannelId,
                    nowNext = epgCache[channel.id],
                    onSelect = { onChannelSelected(channel) },
                    onVisible = { viewModel.requestEpgFor(channel.id) }
                )
            }
        }
    }
}

@Composable
private fun GuideRow(
    channel: Channel,
    isCurrent: Boolean,
    nowNext: EpgNowNext?,
    onSelect: () -> Unit,
    onVisible: () -> Unit
) {
    LaunchedEffect(channel.id) { onVisible() }

    Surface(onClick = onSelect, modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isCurrent) SirKTVPrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.size(28.dp).background(SirKTVPrimary, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                Text(channel.name.take(2).uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "${channel.channelNumber} · ${channel.name}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(180.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nowNext?.now?.title ?: "—",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                nowNext?.now?.let {
                    Text(
                        "${formatEpochSeconds(it.startEpochSeconds)} – ${formatEpochSeconds(it.endEpochSeconds)}",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp
                    )
                }
            }
            nowNext?.next?.let {
                Column(horizontalAlignment = Alignment.End) {
                    Text("NEXT", color = SirKTVPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(it.title, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
