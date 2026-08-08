package com.sirktv.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text as TvText
import com.sirktv.app.presentation.common.tvFocusStyle
import com.sirktv.app.presentation.theme.Dimens
import com.sirktv.app.presentation.theme.SirKTVError
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVPrimary

/** Embedded inline as the expanded panel for the "Parental Controls" settings tile. */
@Composable
fun ParentalControlsPanel(viewModel: ParentalControlsViewModel = hiltViewModel()) {
    val isPinLockEnabled by viewModel.isPinLockEnabled.collectAsState()
    var isSettingUpPin by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg)) {
            SettingsSectionCard {
                SettingsToggleRow(
                    label = "PIN Lock",
                    subtitle = "Require a PIN to open locked categories and content",
                    checked = isPinLockEnabled,
                    onCheckedChange = { enabling ->
                        if (enabling) {
                            isSettingUpPin = true
                        } else {
                            viewModel.disablePinLock()
                        }
                    }
                )
                if (isSettingUpPin) {
                    PinSetupForm(
                        onCancel = { isSettingUpPin = false },
                        onConfirm = { pin ->
                            viewModel.enablePinLock(pin)
                            isSettingUpPin = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinSetupForm(onCancel: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val mismatch = confirmPin.isNotEmpty() && pin != confirmPin
    val valid = pin.length in 4..8 && pin == confirmPin

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
            label = { Text("New PIN (4-8 digits)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SirKTVPrimary,
                unfocusedBorderColor = SirKTVOnSurfaceMuted,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().tvFocusStyle()
        )
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) confirmPin = it },
            label = { Text("Confirm PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SirKTVPrimary,
                unfocusedBorderColor = SirKTVOnSurfaceMuted,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().tvFocusStyle()
        )
        if (mismatch) {
            Text("PINs don't match.", color = SirKTVError, fontSize = 12.sp)
        }
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            Button(onClick = onCancel, modifier = Modifier.tvFocusStyle()) { TvText("Cancel") }
            Button(
                onClick = { onConfirm(pin) },
                enabled = valid,
                colors = ButtonDefaults.colors(containerColor = SirKTVPrimary, contentColor = Color.White),
                modifier = Modifier.tvFocusStyle()
            ) { TvText("Save PIN") }
        }
    }
}
