package com.sirktv.app.presentation.livetv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.domain.model.SubscriptionStatus
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun LiveTvScreen(
    onLoggedOut: () -> Unit,
    viewModel: LiveTvViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LiveTvEvent.NavigateToLogin -> onLoggedOut()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)
        ) {
            Text(
                text = "You're connected",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = SirKTVPrimary
            )

            profile?.let { p ->
                Text("Signed in as ${p.username}", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
                Text(p.serverUrl, fontSize = 13.sp, color = SirKTVOnSurfaceMuted)
                Text(
                    text = subscriptionSummary(p.status, p.isTrial, p.maxConnections),
                    fontSize = 13.sp,
                    color = SirKTVOnSurfaceMuted
                )
            }

            Text(
                text = "Live TV, EPG, and playback arrive in the next phase.",
                fontSize = 13.sp,
                color = SirKTVOnSurfaceMuted
            )

            Button(
                onClick = viewModel::onLogoutClicked,
                modifier = Modifier.tvFocusStyle()
            ) {
                Text("Log Out")
            }
        }
    }
}

private fun subscriptionSummary(status: SubscriptionStatus, isTrial: Boolean, maxConnections: Int): String {
    val trialLabel = if (isTrial) " (trial)" else ""
    return "Status: ${status.name.lowercase().replaceFirstChar { it.uppercase() }}$trialLabel · $maxConnections connection(s)"
}
