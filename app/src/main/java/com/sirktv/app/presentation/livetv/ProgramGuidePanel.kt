package com.sirktv.app.presentation.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Surface
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary

private val ChannelColumnWidth = 168.dp
private val RowHeight = 60.dp
private val PxPerMinute = 4.dp
private const val WindowHours = 6
private const val SlotMinutes = 30

/**
 * Pixel-positioned EPG timeline: a fixed channel column on the left, a time
 * ruler pinned above, and a horizontally + vertically scrollable grid of
 * program blocks whose x-offset and width are computed directly from each
 * program's start/end time against a shared minute→dp scale. The ruler and
 * every row share one [rememberScrollState] instance, so dragging any row
 * (or the ruler) pans all of them together without a custom Layout.
 */
@Composable
fun ProgramGuidePanel(
    channels: List<Channel>,
    currentChannelId: String?,
    onChannelSelected: (Channel) -> Unit,
    viewModel: LiveTvPlayerViewModel = hiltViewModel()
) {
    val listingsCache by viewModel.channelEpgListingsCache.collectAsState()
    val listState = rememberLazyListState()
    val hScroll = rememberScrollState()
    val currentIndex = channels.indexOfFirst { it.id == currentChannelId }.takeIf { it >= 0 } ?: 0

    val nowSeconds = remember { System.currentTimeMillis() / 1000 }
    val windowStart = remember(nowSeconds) { nowSeconds - (nowSeconds % (SlotMinutes * 60L)) }
    val windowEnd = windowStart + WindowHours * 3600L
    val totalMinutes = ((windowEnd - windowStart) / 60).toInt()
    val gridWidth = (totalMinutes * PxPerMinute.value).dp

    LaunchedEffect(channels) {
        if (currentIndex > 0) listState.scrollToItem(currentIndex)
    }

    Box(Modifier.fillMaxSize().background(Color(0xF205070C))) {
        Column(Modifier.fillMaxSize().padding(top = Dimens.SafeAreaVertical)) {
            Text(
                "Program Guide",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = Dimens.SafeAreaHorizontal, vertical = 8.dp)
            )

            Row(modifier = Modifier.padding(start = Dimens.SafeAreaHorizontal, end = Dimens.SafeAreaHorizontal)) {
                Spacer(Modifier.width(ChannelColumnWidth))
                Box(Modifier.weight(1f).horizontalScroll(hScroll)) {
                    TimeRuler(windowStart = windowStart, nowSeconds = nowSeconds, gridWidth = gridWidth, totalMinutes = totalMinutes)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().background(Color(0xFF05070C)),
                contentPadding = PaddingValues(
                    start = Dimens.SafeAreaHorizontal,
                    end = Dimens.SafeAreaHorizontal,
                    bottom = Dimens.SafeAreaVertical
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    GuideGridRow(
                        channel = channel,
                        isCurrent = channel.id == currentChannelId,
                        programs = listingsCache[channel.id].orEmpty(),
                        windowStart = windowStart,
                        windowEnd = windowEnd,
                        nowSeconds = nowSeconds,
                        gridWidth = gridWidth,
                        hScroll = hScroll,
                        onSelect = { onChannelSelected(channel) },
                        onVisible = { viewModel.requestEpgListingsFor(channel.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRuler(windowStart: Long, nowSeconds: Long, gridWidth: androidx.compose.ui.unit.Dp, totalMinutes: Int) {
    Box(Modifier.width(gridWidth).height(24.dp)) {
        Row(Modifier.fillMaxSize()) {
            var slotStart = windowStart
            val slotWidth = (SlotMinutes * PxPerMinute.value).dp
            while (slotStart < windowStart + totalMinutes * 60L) {
                Box(Modifier.width(slotWidth)) {
                    Text(formatEpochSeconds(slotStart), color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                }
                slotStart += SlotMinutes * 60L
            }
        }
        NowLine(windowStart = windowStart, nowSeconds = nowSeconds, totalMinutes = totalMinutes, modifier = Modifier.fillMaxHeight())
    }
}

@Composable
private fun GuideGridRow(
    channel: Channel,
    isCurrent: Boolean,
    programs: List<EpgProgram>,
    windowStart: Long,
    windowEnd: Long,
    nowSeconds: Long,
    gridWidth: androidx.compose.ui.unit.Dp,
    hScroll: androidx.compose.foundation.ScrollState,
    onSelect: () -> Unit,
    onVisible: () -> Unit
) {
    LaunchedEffect(channel.id) { onVisible() }
    val totalMinutes = ((windowEnd - windowStart) / 60).toInt()

    Row(Modifier.fillMaxWidth().height(RowHeight), verticalAlignment = Alignment.CenterVertically) {
        Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), onClick = onSelect, modifier = Modifier.width(ChannelColumnWidth).fillMaxHeight().tvFocusStyle(cornerRadius = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isCurrent) SirKTVPrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(24.dp).background(SirKTVPrimary, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Text(channel.name.take(2).uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(
                        text = channel.channelNumber.toString(),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxHeight().horizontalScroll(hScroll)) {
            Box(Modifier.width(gridWidth).fillMaxHeight()) {
                programs.forEach { program ->
                    val clippedStart = maxOf(program.startEpochSeconds, windowStart)
                    val clippedEnd = minOf(program.endEpochSeconds, windowEnd)
                    if (clippedEnd <= clippedStart) return@forEach

                    val xOffsetMinutes = (clippedStart - windowStart) / 60f
                    val widthMinutes = (clippedEnd - clippedStart) / 60f
                    val isLive = nowSeconds in program.startEpochSeconds until program.endEpochSeconds

                    Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), 
                        onClick = onSelect,
                        modifier = Modifier
                            .offset(x = (xOffsetMinutes * PxPerMinute.value).dp)
                            .width((widthMinutes * PxPerMinute.value).dp.coerceAtLeast(32.dp))
                            .fillMaxHeight()
                            .padding(horizontal = 1.dp)
                            .tvFocusStyle(cornerRadius = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isLive) SirKTVPrimary.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(6.dp)
                        ) {
                            Text(
                                text = program.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isLive) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                NowLine(windowStart = windowStart, nowSeconds = nowSeconds, totalMinutes = totalMinutes, modifier = Modifier.fillMaxHeight())
            }
        }
    }
}

@Composable
private fun NowLine(windowStart: Long, nowSeconds: Long, totalMinutes: Int, modifier: Modifier = Modifier) {
    if (nowSeconds < windowStart || nowSeconds > windowStart + totalMinutes * 60L) return
    val offsetDp = ((nowSeconds - windowStart) / 60f * PxPerMinute.value).dp
    Box(modifier.offset(x = offsetDp).width(2.dp).background(SirKTVPrimary))
}
