package com.sirktv.app.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sirktv.app.presentation.common.SirKTVLogoMark
import com.sirktv.app.presentation.common.TvFocusAccent
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVError
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary
import com.sirktv.app.presentation.theme.SirKTVSurface
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Switch
import androidx.tv.material3.Text as TvText
import kotlinx.coroutines.delay

/**
 * Explicit D-pad Up/Down wiring for a focus target, applied via
 * [androidx.compose.ui.input.key.onPreviewKeyEvent] rather than
 * `Modifier.focusProperties { up/down = ... }`. focusProperties hints are only
 * consulted when the focus system's own `moveFocus()` runs for an unconsumed
 * directional key — but BasicTextField (which OutlinedTextField wraps) can
 * swallow DirectionUp/Down itself first, so those hints silently never fire
 * on the physical remote. Intercepting in the preview (capture) phase runs
 * before that inner consumption ever gets a chance.
 */
private fun Modifier.tvDpadNav(up: FocusRequester? = null, down: FocusRequester? = null): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionUp -> up?.let { it.requestFocus(); true } ?: false
            Key.DirectionDown -> down?.let { it.requestFocus(); true } ?: false
            else -> false
        }
    }

@Composable
fun LoginScreen(
    onNavigateToLiveTv: (channelId: String) -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToLiveTv -> onNavigateToLiveTv(event.channelId)
                LoginEvent.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    // While silently re-authenticating a saved session, show only the brand —
    // never the login form card — so a returning user sees something closer
    // to a TV turning on than an app booting up.
    if (uiState.isReconnecting) {
        BrandedSplash()
    } else {
        LoginContent(
            uiState = uiState,
            onServerUrlChanged = viewModel::onServerUrlChanged,
            onUsernameChanged = viewModel::onUsernameChanged,
            onPasswordChanged = viewModel::onPasswordChanged,
            onDisplayNameChanged = viewModel::onDisplayNameChanged,
            onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
            onToggleRememberMe = viewModel::onToggleRememberMe,
            onToggleDisclaimer = viewModel::onToggleDisclaimer,
            onSignInClicked = viewModel::onSignInClicked
        )
    }
}

@Composable
private fun BrandedSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SirKTVBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            SirKTVLogoMark()
            Text(
                text = "Your World. Your Channels.",
                fontSize = 13.sp,
                color = SirKTVOnSurfaceMuted
            )
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = SirKTVPrimary)
        }
    }
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onServerUrlChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleRememberMe: () -> Unit,
    onToggleDisclaimer: () -> Unit,
    onSignInClicked: () -> Unit
) {
    // One FocusRequester per stop so D-pad Up/Down and IME Next/Done can be
    // pinned to an exact chain (serverUrl -> username -> password ->
    // displayName -> rememberMe -> signIn -> disclaimer) instead of relying
    // on default 2D focus search, which the password field's trailing
    // SHOW/HIDE button could otherwise throw off.
    val serverUrlFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val displayNameFocusRequester = remember { FocusRequester() }
    val rememberMeFocusRequester = remember { FocusRequester() }
    val signInFocusRequester = remember { FocusRequester() }
    val disclaimerFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(300) // wait for composition before the first requestFocus()
        serverUrlFocusRequester.requestFocus()
        keyboardController?.show()
    }

    // Top-aligned and scrollable at the screen level (not just inside the
    // card) — a Box that centers its content vertically would clip the
    // bottom of the card (Sign In, the error message, the disclaimer) any
    // time the card's natural height exceeds the viewport, since centering
    // pushes the overflow equally off both the top and bottom edges.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Dimens.SafeAreaHorizontal, vertical = Dimens.SafeAreaVertical),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.width(440.dp),
            shape = RoundedCornerShape(Dimens.CornerRadius * 2),
            color = SirKTVSurface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(Dimens.SpaceXl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
                ) {
                    SirKTVLogoMark()
                    Column {
                        Text(
                            text = "SirKTV",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = SirKTVPrimary
                        )
                        Text(
                            text = "Sign in with your Xtream Codes account",
                            fontSize = 11.sp,
                            color = SirKTVOnSurfaceMuted
                        )
                    }
                }

                if (uiState.isReconnecting) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Reconnecting to your saved account...", fontSize = 12.sp, color = SirKTVOnSurfaceMuted)
                    }
                }

                OutlinedTextField(
                    value = uiState.serverUrl,
                    onValueChange = onServerUrlChanged,
                    label = { Text("Server address") },
                    placeholder = { Text("http://example.com:8080") },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { usernameFocusRequester.requestFocus() }),
                    colors = loginFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(serverUrlFocusRequester)
                        .tvDpadNav(down = usernameFocusRequester)
                        // D-pad focus alone doesn't pop the Fire TV system
                        // keyboard the way a touch tap would — show it
                        // explicitly the moment this field gains focus.
                        .onFocusChanged { if (it.isFocused) keyboardController?.show() }
                        .tvFocusStyle()
                )

                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = onUsernameChanged,
                    label = { Text("Username") },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                    colors = loginFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(usernameFocusRequester)
                        .tvDpadNav(up = serverUrlFocusRequester, down = passwordFocusRequester)
                        .onFocusChanged { if (it.isFocused) keyboardController?.show() }
                        .tvFocusStyle()
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { displayNameFocusRequester.requestFocus() }),
                    visualTransformation = if (uiState.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = onTogglePasswordVisibility) {
                            Text(if (uiState.isPasswordVisible) "HIDE" else "SHOW", fontSize = 11.sp)
                        }
                    },
                    colors = loginFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .tvDpadNav(up = usernameFocusRequester, down = displayNameFocusRequester)
                        .onFocusChanged { if (it.isFocused) keyboardController?.show() }
                        .tvFocusStyle()
                )

                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = onDisplayNameChanged,
                    label = { Text("Display Name (optional)") },
                    placeholder = { Text("How should we greet you?") },
                    supportingText = { Text("Leave blank to use your username instead.", fontSize = 11.sp) },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { rememberMeFocusRequester.requestFocus() }),
                    colors = loginFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(displayNameFocusRequester)
                        .tvDpadNav(up = passwordFocusRequester, down = rememberMeFocusRequester)
                        .onFocusChanged { if (it.isFocused) keyboardController?.show() }
                        .tvFocusStyle()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)
                ) {
                    Switch(
                        checked = uiState.rememberMe,
                        onCheckedChange = { onToggleRememberMe() },
                        modifier = Modifier
                            .focusRequester(rememberMeFocusRequester)
                            .tvDpadNav(up = displayNameFocusRequester, down = signInFocusRequester)
                            .tvFocusStyle(cornerRadius = 24.dp)
                    )
                    Text("Remember me on this device", fontSize = 13.sp, color = SirKTVOnSurfaceMuted)
                }

                Button(
                    onClick = onSignInClicked,
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.colors(
                        containerColor = SirKTVPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .focusRequester(signInFocusRequester)
                        .tvDpadNav(up = rememberMeFocusRequester, down = disclaimerFocusRequester)
                        .tvFocusStyle(accent = TvFocusAccent.BORDER)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        TvText("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = SirKTVError,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                DisclaimerSection(
                    expanded = uiState.isDisclaimerExpanded,
                    onToggle = onToggleDisclaimer,
                    focusRequester = disclaimerFocusRequester,
                    upFocusRequester = signInFocusRequester
                )
            }
        }
    }
}

@Composable
private fun DisclaimerSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester
) {
    // 16dp top spacing below Sign In, matching the card's own internal rhythm.
    Column(modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpaceMd)) {
        TextButton(
            onClick = onToggle,
            modifier = Modifier
                .focusRequester(focusRequester)
                .tvDpadNav(up = upFocusRequester)
                .tvFocusStyle(cornerRadius = 6.dp)
        ) {
            Text(
                text = if (expanded) "Disclaimer ▾" else "Disclaimer ▸",
                color = SirKTVOnSurfaceMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (expanded) {
            Text(
                text = "SirKTV is a media player only and does not host, provide, or distribute any" +
                    " content. All streams are supplied by the third-party service you sign in with" +
                    " using your own account credentials. SirKTV is not responsible for the" +
                    " availability, legality, or quality of any content accessed through your" +
                    " provider. You are solely responsible for ensuring your use of any connected" +
                    " service complies with the laws and regulations applicable to you.",
                color = SirKTVOnSurfaceMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.SpaceXs, vertical = Dimens.SpaceXs)
            )
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SirKTVBackground,
    unfocusedContainerColor = SirKTVBackground,
    disabledContainerColor = SirKTVBackground,
    focusedBorderColor = SirKTVPrimary,
    unfocusedBorderColor = SirKTVOnSurfaceMuted,
    focusedLabelColor = SirKTVPrimary,
    cursorColor = SirKTVPrimary,
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
)
