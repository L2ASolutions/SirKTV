package com.sirktv.app.presentation.livetv

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary

@Composable
fun ChannelListPanel(
    channels: List<Channel>,
    currentChannelId: String?,
    onChannelSelected: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    viewModel: LiveTvPlayerViewModel = hiltViewModel()
) {
    val epgCache by viewModel.channelEpgCache.collectAsState()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val filteredChannels = if (normalizedQuery.isEmpty()) {
        channels
    } else {
        channels.filter { it.name.contains(normalizedQuery, ignoreCase = true) }
    }
    val favorites = filteredChannels.filter { it.isFavorite }

    fun toggleWithToast(channel: Channel) {
        Toast.makeText(
            context,
            if (channel.isFavorite) "Removed from Favorites" else "Added to Favorites",
            Toast.LENGTH_SHORT
        ).show()
        onFavoriteToggle(channel)
    }

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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (favorites.isNotEmpty()) {
                    item { SectionHeader("Favorites") }
                    items(favorites, key = { "fav-${it.id}" }) { channel ->
                        LaunchedEffect(channel.id) { viewModel.requestEpgFor(channel.id) }
                        ChannelCard(
                            channel = channel,
                            nowNext = epgCache[channel.id],
                            isCurrent = channel.id == currentChannelId,
                            onClick = { onChannelSelected(channel) },
                            onToggleFavorite = { toggleWithToast(channel) },
                            modifier = Modifier.fillMaxWidth()
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
                    LaunchedEffect(channel.id) { viewModel.requestEpgFor(channel.id) }
                    ChannelCard(
                        channel = channel,
                        nowNext = epgCache[channel.id],
                        isCurrent = channel.id == currentChannelId,
                        onClick = { onChannelSelected(channel) },
                        onToggleFavorite = { toggleWithToast(channel) },
                        modifier = Modifier.fillMaxWidth()
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
