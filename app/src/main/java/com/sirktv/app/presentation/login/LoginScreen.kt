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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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

/** Minimum distance the login card is kept from the screen edges on TV. */
private val LoginSafeZone = 48.dp

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
            onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
            onToggleRememberMe = viewModel::onToggleRememberMe,
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
    onTogglePasswordVisibility: () -> Unit,
    onToggleRememberMe: () -> Unit,
    onSignInClicked: () -> Unit
) {
    // One FocusRequester per stop so D-pad Up/Down and IME Next/Done can be
    // pinned to an exact chain (serverUrl -> username -> password ->
    // rememberMe -> signIn) instead of relying on default 2D focus search,
    // which the password field's trailing SHOW/HIDE button could otherwise
    // throw off.
    val serverUrlFocusRequester = remember { FocusRequester() }
    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val rememberMeFocusRequester = remember { FocusRequester() }
    val signInFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        serverUrlFocusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = LoginSafeZone, vertical = LoginSafeZone),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.width(440.dp),
            shape = RoundedCornerShape(Dimens.CornerRadius * 2),
            color = SirKTVSurface,
            tonalElevation = 4.dp
        ) {
            // verticalScroll is a safety net only — on-target spacing below is
            // sized to already fit a 10-foot TV viewport with no scrolling, but
            // this keeps every field (especially Sign In) D-pad reachable even
            // on a smaller/denser screen than tested against.
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = Dimens.SpaceLg, vertical = Dimens.SpaceMd),
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
                        .focusProperties { down = usernameFocusRequester }
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
                        .focusProperties {
                            up = serverUrlFocusRequester
                            down = passwordFocusRequester
                        }
                        .onFocusChanged { if (it.isFocused) keyboardController?.show() }
                        .tvFocusStyle()
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChanged,
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { signInFocusRequester.requestFocus() }),
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
                        .focusProperties {
                            up = usernameFocusRequester
                            down = rememberMeFocusRequester
                        }
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
                            .focusProperties {
                                up = passwordFocusRequester
                                down = signInFocusRequester
                            }
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
                        .focusProperties { up = rememberMeFocusRequester }
                        .tvFocusStyle()
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
            }
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
