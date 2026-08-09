package com.sirktv.app.presentation.livetv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Tracks
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVSurface
import androidx.tv.material3.Surface

@Composable
fun TrackSelectorSheet(
    title: String,
    tracks: Tracks?,
    trackType: Int,
    allowOff: Boolean,
    onSelect: (Tracks.Group, Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    val groups = tracks?.groups.orEmpty().filter { it.type == trackType }
    val anySelected = groups.any { group -> (0 until group.length).any { group.isTrackSelected(it) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(SirKTVSurface)
                .padding(Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)

            if (groups.isEmpty()) {
                Text("Not available for this stream", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
            }

            if (allowOff) {
                TrackOptionRow(label = "Off", selected = !anySelected, onClick = onClear)
            }

            groups.forEachIndexed { groupIndex, group ->
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val label = format.label ?: format.language?.uppercase() ?: "Track ${groupIndex + 1}.${trackIndex + 1}"
                    TrackOptionRow(
                        label = label,
                        selected = group.isTrackSelected(trackIndex),
                        onClick = { onSelect(group, trackIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), 
        onClick = onClick,
        modifier = Modifier.tvFocusStyle(cornerRadius = 8.dp)
    ) {
        Text(
            text = if (selected) "● $label" else label,
            color = if (selected) SirKTVPrimary else Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

/** [C.TRACK_TYPE_AUDIO] / [C.TRACK_TYPE_TEXT] re-exported for callers that only need the constants. */
object TrackTypes {
    const val AUDIO = C.TRACK_TYPE_AUDIO
    const val TEXT = C.TRACK_TYPE_TEXT
}
