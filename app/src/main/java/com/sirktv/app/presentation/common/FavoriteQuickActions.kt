package com.sirktv.app.presentation.common

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVError
import com.sirktv.app.presentation.theme.SirKTVSurface
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText

/**
 * Long-press-OK quick actions for a favorited item: Play, and Remove from
 * Favorites (which asks for a yes/no confirmation before actually removing,
 * rather than removing immediately on a single accidental long press).
 */
@Composable
fun FavoriteQuickActionsSheet(
    itemTitle: String,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var confirmingRemove by remember { mutableStateOf(false) }

    BackHandler {
        if (confirmingRemove) confirmingRemove = false else onDismiss()
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
        if (confirmingRemove) {
            ConfirmDialog(
                message = "Remove \"$itemTitle\" from favorites?",
                confirmLabel = "Remove",
                onConfirm = { onRemove(); onDismiss() },
                onCancel = { confirmingRemove = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .background(SirKTVSurface, RoundedCornerShape(16.dp))
                    .padding(Dimens.SpaceLg),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
            ) {
                Text(
                    itemTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                QuickActionRow(label = "▶  Play") { onPlay(); onDismiss() }
                QuickActionRow(label = "♥  Remove from Favorites") { confirmingRemove = true }
                QuickActionRow(label = "Cancel", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun QuickActionRow(label: String, onClick: () -> Unit) {
    Surface(border = com.sirktv.app.presentation.common.tvNoBorder(), onClick = onClick, modifier = Modifier.fillMaxWidth().tvFocusStyle(cornerRadius = 8.dp)) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
    }
}

/** TV-friendly yes/no confirmation dialog — Confirm button is destructive-red, Cancel is the safe default focus target. */
@Composable
fun ConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    confirmLabel: String = "Yes",
    cancelLabel: String = "Cancel"
) {
    BackHandler(onBack = onCancel)

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .background(SirKTVSurface, RoundedCornerShape(16.dp))
                .padding(Dimens.SpaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
        ) {
            Text(
                message,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Button(border = com.sirktv.app.presentation.common.tvNoButtonBorder(), 
                    onClick = onCancel,
                    modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)
                ) { TvText(cancelLabel) }
                Button(border = com.sirktv.app.presentation.common.tvNoButtonBorder(), 
                    onClick = onConfirm,
                    colors = androidx.tv.material3.ButtonDefaults.colors(containerColor = SirKTVError, contentColor = Color.White),
                    modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)
                ) { TvText(confirmLabel) }
            }
        }
    }
}
