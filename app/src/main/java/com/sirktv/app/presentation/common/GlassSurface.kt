package com.sirktv.app.presentation.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVSurfaceElevated
import com.sirktv.app.presentation.theme.SirKTVTextPrimary
import com.sirktv.app.presentation.theme.SirKTVTextTertiary
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

/**
 * Filter chip shared by every category row (Movies, Series, Search) and the
 * nav pill row — Apple-TV style: selected = solid accent fill, full pill
 * shape, no border. Unselected floats with no container at all (fully
 * transparent) until it gains D-pad focus, at which point a subtle
 * `SirKTVSurfaceElevated` wash appears and the text brightens to
 * `SirKTVTextPrimary` — never a border, only the shared glow from
 * [tvFocusStyle] plus this background wash.
 */
@Composable
fun CategoryPill(label: String, selected: Boolean, accent: Color = SirKTVPrimary, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        targetValue = when {
            selected -> accent
            isFocused -> SirKTVSurfaceElevated
            else -> Color.Transparent
        },
        label = "categoryPillBackground"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            selected -> Color.White
            isFocused -> SirKTVTextPrimary
            else -> SirKTVTextTertiary
        },
        label = "categoryPillTextColor"
    )
    Surface(onClick = onClick, modifier = Modifier.tvFocusStyle(cornerRadius = 999.dp) { isFocused = it }) {
        Box(
            modifier = Modifier
                .background(background, RoundedCornerShape(999.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(text = label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
