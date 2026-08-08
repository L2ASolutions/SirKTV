package com.sirktv.app.presentation.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.common.tvPressLongPress
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import androidx.tv.material3.Surface

/**
 * The one Live TV channel card used everywhere a channel shows up as a tile
 * or row (Home's Live TV/Favorite Channels rows, the in-player channel list,
 * the Live TV browse screen, and the Favorites screen's Channels tab): logo
 * on the left, name + EPG now-playing + progress bar (or a "Live" badge when
 * no EPG data is available) in the middle, optional favorite heart on the
 * right. Always a dark navy (#1A1A2E) card so text stays readable regardless
 * of what's behind it.
 */
@Composable
fun ChannelCard(
    channel: Channel,
    nowNext: EpgNowNext?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrent: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    // See MediaCard's onLongClick doc — same opt-in Surface-vs-Box tradeoff.
    onLongClick: (() -> Unit)? = null
) {
    val content = @Composable { ChannelCardRow(channel, nowNext, isCurrent, onToggleFavorite) }
    if (onLongClick != null) {
        Box(modifier = modifier.tvFocusStyle(cornerRadius = 12.dp).tvPressLongPress(onClick = onClick, onLongPress = onLongClick)) {
            content()
        }
    } else {
        Surface(onClick = onClick, modifier = modifier.tvFocusStyle(cornerRadius = 12.dp)) {
            content()
        }
    }
}

@Composable
private fun ChannelCardRow(
    channel: Channel,
    nowNext: EpgNowNext?,
    isCurrent: Boolean,
    onToggleFavorite: (() -> Unit)?
) {
    val now = nowNext?.now
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
                    .height(32.dp)
                    .background(if (isCurrent) SirKTVPrimary else Color.Transparent, RoundedCornerShape(2.dp))
            )

            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(SirKTVPrimary, SirKTVPrimary.copy(alpha = 0.6f))),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(channel.name.take(2).uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .background(SirKTVPrimary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("LIVE", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (now != null) {
                    Text(
                        text = now.title,
                        color = SirKTVOnSurfaceMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    ProgramProgressBar(now, modifier = Modifier.padding(top = 4.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("Live", color = SirKTVOnSurfaceMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

        onToggleFavorite?.let { toggle ->
            FavoriteHeartButton(isFavorite = channel.isFavorite, onToggle = toggle, modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
fun FavoriteHeartButton(isFavorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onToggle, modifier = modifier.tvFocusStyle(cornerRadius = 8.dp)) {
        Text(
            text = if (isFavorite) "♥" else "♡",
            color = if (isFavorite) SirKTVPrimary else Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp,
            modifier = Modifier.padding(6.dp)
        )
    }
}

@Composable
private fun ProgramProgressBar(program: EpgProgram, modifier: Modifier = Modifier) {
    val fraction = program.progressFraction()
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .weight(1f)
                .height(3.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
        ) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(SirKTVPrimary, RoundedCornerShape(999.dp)))
        }
        program.minutesRemaining()?.let {
            Text("${it}m left", color = SirKTVOnSurfaceMuted, fontSize = 9.sp)
        }
    }
}

private fun EpgProgram.progressFraction(): Float {
    val total = (endEpochSeconds - startEpochSeconds).toFloat()
    if (total <= 0f) return 0f
    val nowSeconds = System.currentTimeMillis() / 1000
    return ((nowSeconds - startEpochSeconds).toFloat() / total).coerceIn(0f, 1f)
}

private fun EpgProgram.minutesRemaining(): Long? {
    val nowSeconds = System.currentTimeMillis() / 1000
    val remaining = endEpochSeconds - nowSeconds
    return if (remaining > 0) (remaining / 60).coerceAtLeast(1) else null
}
