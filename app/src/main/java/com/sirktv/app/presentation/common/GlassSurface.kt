package com.sirktv.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary
import androidx.tv.material3.Surface

/**
 * Frosted-glass card treatment shared by Movies/Series/Favorites/Search grids
 * and player panels: a soft translucent gradient fill plus a hairline
 * highlight border. Deliberately not a real-time backdrop blur
 * (Modifier.blur/RenderEffect) — this targets Fire Stick-class hardware,
 * which is exactly what the playback engineering spec is otherwise careful
 * not to burden with extra GPU work.
 */
fun Modifier.glassCard(cornerRadius: Dp = Dimens.CornerRadius): Modifier = this
    .background(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f))),
        RoundedCornerShape(cornerRadius)
    )
    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(cornerRadius))

/** Filter chip shared by every category row (Movies, Series, Search). Selected = solid accent; unselected = glass. */
@Composable
fun CategoryPill(label: String, selected: Boolean, accent: Color = SirKTVPrimary, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.tvFocusStyle(cornerRadius = 999.dp)) {
        Box(
            modifier = if (selected) {
                Modifier.background(accent, RoundedCornerShape(999.dp))
            } else {
                Modifier.glassCard(999.dp)
            }.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
