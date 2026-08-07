package com.sirktv.app.presentation.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVError
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun HubScreen(
    onNavigateToLiveTv: (channelId: String) -> Unit,
    onNavigateToSports: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: HubViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    var infoMessage by remember { mutableStateOf<String?>(null) }
    val liveTvFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { liveTvFocusRequester.requestFocus() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HubEvent.NavigateToLiveTv -> onNavigateToLiveTv(event.channelId)
                HubEvent.NavigateToSports -> onNavigateToSports()
                HubEvent.NavigateToSettings -> onNavigateToSettings()
                HubEvent.NavigateToLogin -> onLoggedOut()
                HubEvent.NoChannelsAvailable -> infoMessage = "No live channels found on this account yet."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground)
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)) {
            SirKTVLogoMark()

            profile?.let { p ->
                Text(
                    text = "Welcome back, ${p.username}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                HubTile("Live TV", onClick = viewModel::onLiveTvClicked, modifier = Modifier.focusRequester(liveTvFocusRequester))
                HubTile("Sports", onClick = viewModel::onSportsClicked)
                HubTile("Settings", onClick = viewModel::onSettingsClicked)
            }

            infoMessage?.let { message ->
                Text(text = message, color = SirKTVError, fontSize = 13.sp)
            }

            Button(onClick = viewModel::onLogoutClicked, modifier = Modifier.tvFocusStyle()) {
                Text("Log Out")
            }
        }
    }
}

@Composable
private fun HubTile(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .height(96.dp)
            .tvFocusStyle()
    ) {
        Text(label)
    }
}
