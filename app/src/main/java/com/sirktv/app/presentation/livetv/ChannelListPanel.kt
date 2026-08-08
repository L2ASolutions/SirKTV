package com.sirktv.app.presentation.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import androidx.tv.material3.Surface

@Composable
fun ChannelListPanel(
    channels: List<Channel>,
    currentChannelId: String?,
    onChannelSelected: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    viewModel: LiveTvPlayerViewModel = hiltViewModel()
) {
    val epgCache by viewModel.channelEpgCache.collectAsState()
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val filteredChannels = if (normalizedQuery.isEmpty()) {
        channels
    } else {
        channels.filter { it.name.contains(normalizedQuery, ignoreCase = true) }
    }
    val favorites = filteredChannels.filter { it.isFavorite }

    Box(Modifier.fillMaxHeight().width(360.dp).background(Color(0xEE0A0A0F))) {
        Column(modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(vertical = Dimens.SafeAreaVertical, horizontal = Dimens.SpaceMd)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search channels", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = SirKTVPrimary,
                    unfocusedBorderColor = SirKTVOnSurfaceMuted,
                    cursorColor = SirKTVPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(top = Dimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (favorites.isNotEmpty()) {
                    item { SectionHeader("Favorites") }
                    items(favorites, key = { "fav-${it.id}" }) { channel ->
                        ChannelRow(
                            channel = channel,
                            isCurrent = channel.id == currentChannelId,
                            epgTitle = epgCache[channel.id]?.now?.title,
                            onSelect = { onChannelSelected(channel) },
                            onFavoriteToggle = { onFavoriteToggle(channel) },
                            onVisible = { viewModel.requestEpgFor(channel.id) }
                        )
                    }
                }

                item { SectionHeader(if (normalizedQuery.isEmpty()) "All Channels" else "Results") }
                if (filteredChannels.isEmpty()) {
                    item {
                        Text(
                            "No channels match \"$normalizedQuery\".",
                            color = SirKTVOnSurfaceMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                items(filteredChannels, key = { it.id }) { channel ->
                    ChannelRow(
                        channel = channel,
                        isCurrent = channel.id == currentChannelId,
                        epgTitle = epgCache[channel.id]?.now?.title,
                        onSelect = { onChannelSelected(channel) },
                        onFavoriteToggle = { onFavoriteToggle(channel) },
                        onVisible = { viewModel.requestEpgFor(channel.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun ChannelRow(
    channel: Channel,
    isCurrent: Boolean,
    epgTitle: String?,
    onSelect: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onVisible: () -> Unit
) {
    LaunchedEffect(channel.id) { onVisible() }

    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isCurrent) SirKTVPrimary.copy(alpha = 0.22f) else Color.Transparent, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(30.dp).background(SirKTVPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(channel.name.take(2).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = "${channel.channelNumber} · ${channel.name}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = epgTitle ?: " ",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isCurrent) {
                Text("LIVE", color = Color(0xFFFF4757), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Surface(onClick = onFavoriteToggle, modifier = Modifier.tvFocusStyle(cornerRadius = 8.dp)) {
                Text(
                    text = if (channel.isFavorite) "♥" else "♡",
                    color = if (channel.isFavorite) SirKTVPrimary else Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}
