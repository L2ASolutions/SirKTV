package com.sirktv.app.presentation.livetv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVSurface
import androidx.tv.material3.Surface

@Composable
fun QuickActionsSheet(
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onRestartStream: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .background(SirKTVSurface, RoundedCornerShape(16.dp))
                .padding(Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
        ) {
            Text("Quick Actions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            QuickActionRow(label = if (isFavorite) "Remove Favorite" else "Add Favorite") {
                onFavoriteToggle()
                onDismiss()
            }
            QuickActionRow(label = "Restart Stream") {
                onRestartStream()
                onDismiss()
            }
            QuickActionRow(label = "Close", onClick = onDismiss)
        }
    }
}

@Composable
private fun QuickActionRow(label: String, onClick: () -> Unit) {
    Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), glow = com.sirktv.app.presentation.common.tvNoGlow(), onClick = onClick, modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 8.dp)) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}
