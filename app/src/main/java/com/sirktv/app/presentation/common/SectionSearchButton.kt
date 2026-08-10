package com.sirktv.app.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Surface
import com.sirktv.app.presentation.navigation.SirKTVIcons
import com.sirktv.app.presentation.theme.SirKTVTextSecondary

/**
 * The Search icon shown in the top-right corner of every top-level section
 * (Live TV, Movies, Series) that has no other on-screen way to reach Search —
 * without a persistent sidebar/nav row, the only other path in was the Fire
 * TV remote's dedicated SEARCH/mic button, which not every device or
 * accessibility setup surfaces. Presses [onClick], which every call site
 * wires to the app's one shared Search screen — this button intentionally
 * does not open its own search UI, so recent-history, IME handling, and
 * cross-content search all keep coming from that single implementation.
 */
@Composable
fun SectionSearchButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        border = tvNoBorder(),
        onClick = onClick,
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .tvFocusStyle(cornerRadius = 8.dp)
    ) {
        Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
            SirKTVIcons.Search(tint = SirKTVTextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}
