package com.sirktv.app.presentation.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Surface
import androidx.tv.material3.Text as TvText
import com.sirktv.app.presentation.common.SirKTVChrome
import com.sirktv.app.presentation.common.SirKTVNavItem
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVCardBackground
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVPrimaryVariant
import com.sirktv.app.presentation.theme.SirKTVSeriesAccent
import com.sirktv.app.presentation.theme.SirKTVSportsAccent
import com.sirktv.app.presentation.theme.SirKTVSurface
import com.sirktv.app.presentation.theme.SirKTVTextPrimary
import com.sirktv.app.presentation.theme.SirKTVTextSecondary

private val LiveTvCardWidth = 300.dp
private val PosterCardWidth = 140.dp
private val TileHeight = 200.dp

/**
 * Home is a clean launcher, not a browse screen: four large tiles reach the
 * same four sections the nav pills do (no content loads here), plus two Room
 * -only rows that appear silently once local watch-history data exists. Real
 * content sync only ever happens once the user opens a section.
 */
@Composable
fun HomeScreen(
    onOpenContent: (HomeNavTarget) -> Unit,
    onNavigate: (SirKTVNavItem) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    // Home is effectively the root of the back stack after login — Back here
    // must never exit immediately, only after the user confirms.
    BackHandler(enabled = !showExitDialog) { showExitDialog = true }
    BackHandler(enabled = showExitDialog) { showExitDialog = false }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.NavigateToLogin -> onLoggedOut()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(SirKTVBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = Dimens.SafeAreaHorizontal,
                vertical = Dimens.SafeAreaVertical
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)
        ) {
            item {
                SirKTVChrome(activeItem = SirKTVNavItem.HOME, onNavigate = onNavigate, onRefresh = {})
            }

            item {
                HomeTileGrid(onNavigate = onNavigate)
            }

            if (state.continueWatching.isNotEmpty()) {
                item {
                    MediaRow(title = "Continue Watching", rowItems = state.continueWatching) { progress ->
                        MediaCard(
                            title = progress.title,
                            imageUrl = progress.imageUrl,
                            subtitle = progress.subtitle,
                            progressFraction = progress.progressFraction,
                            onClick = { viewModel.watchProgressTarget(progress)?.let(onOpenContent) },
                            modifier = Modifier.width(LiveTvCardWidth)
                        )
                    }
                }
            }

            if (state.recentlyWatched.isNotEmpty()) {
                item {
                    MediaRow(title = "Recently Watched", rowItems = state.recentlyWatched) { progress ->
                        MediaCard(
                            title = progress.title,
                            imageUrl = progress.imageUrl,
                            aspectRatio = 2f / 3f,
                            onClick = { viewModel.watchProgressTarget(progress)?.let(onOpenContent) },
                            modifier = Modifier.width(PosterCardWidth)
                        )
                    }
                }
            }
        }

        if (showExitDialog) {
            ExitConfirmationDialog(
                onConfirm = { activity?.finish() },
                onDismiss = { showExitDialog = false }
            )
        }
    }
}

@Composable
private fun HomeTileGrid(onNavigate: (SirKTVNavItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            HomeTile(
                title = "Live TV",
                subtitle = "Browse live channels",
                icon = "📡",
                accentColor = SirKTVPrimary,
                onClick = { onNavigate(SirKTVNavItem.LIVE_TV) },
                modifier = Modifier.weight(1f)
            )
            HomeTile(
                title = "Movies",
                subtitle = "Browse movies",
                icon = "🎬",
                accentColor = SirKTVPrimaryVariant,
                onClick = { onNavigate(SirKTVNavItem.MOVIES) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            HomeTile(
                title = "Series",
                subtitle = "Browse TV series",
                icon = "📺",
                accentColor = SirKTVSeriesAccent,
                onClick = { onNavigate(SirKTVNavItem.SERIES) },
                modifier = Modifier.weight(1f)
            )
            HomeTile(
                title = "Sports",
                subtitle = "Browse sports",
                icon = "🏆",
                accentColor = SirKTVSportsAccent,
                onClick = { onNavigate(SirKTVNavItem.SPORTS) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeTile(
    title: String,
    subtitle: String,
    icon: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(TileHeight)
            .tvFocusStyle(cornerRadius = Dimens.CardCornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(SirKTVSurface, SirKTVCardBackground)),
                    RoundedCornerShape(Dimens.CardCornerRadius)
                )
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        accentColor,
                        RoundedCornerShape(topStart = Dimens.CardCornerRadius, bottomStart = Dimens.CardCornerRadius)
                    )
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(Dimens.SpaceLg),
                verticalArrangement = Arrangement.Center
            ) {
                Text(icon, fontSize = 32.sp)
                Text(
                    text = title,
                    color = SirKTVTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Dimens.SpaceSm)
                )
                Text(
                    text = subtitle,
                    color = SirKTVTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val noFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { noFocusRequester.requestFocus() }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .background(SirKTVSurface, RoundedCornerShape(16.dp))
                .padding(Dimens.SpaceLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
        ) {
            Text("Exit SirKTV?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Button(onClick = onDismiss, modifier = Modifier.focusRequester(noFocusRequester).tvFocusStyle(accent = TvFocusAccent.BORDER)) {
                    TvText("No")
                }
                Button(onClick = onConfirm, modifier = Modifier.tvFocusStyle(accent = TvFocusAccent.BORDER)) {
                    TvText("Yes")
                }
            }
        }
    }
}
