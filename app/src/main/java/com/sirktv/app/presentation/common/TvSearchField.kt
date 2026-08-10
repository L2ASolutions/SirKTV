package com.sirktv.app.presentation.common

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.sirktv.app.presentation.theme.SirKTVBackground
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceMuted
import com.sirktv.app.presentation.theme.SirKTVOnSurfaceStrong
import com.sirktv.app.presentation.theme.SirKTVPrimary

/**
 * The one always-visible search input shared by Movies, Series, and the
 * global Search screen. Fire TV's D-pad focus traversal lands on any
 * focusable view it passes over — including this field — so a plain
 * TextField pops its software keyboard open every time focus merely passes
 * through on the way to something else. To avoid that:
 *  - the field starts each focus pass in `readOnly` mode, which lets it take
 *    D-pad focus (and draw the focused border) without starting an input
 *    session, so no keyboard;
 *  - only an explicit DPAD_CENTER/Enter press flips it into edit mode and
 *    shows the keyboard;
 *  - losing focus, pressing DPAD_DOWN, or submitting the search all flip it
 *    back to read-only and hide the keyboard, and DPAD_DOWN also hands focus
 *    straight to [firstResultFocusRequester] so browsing results can never
 *    bounce back into an editing session.
 */
@Composable
fun TvSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fieldFocusRequester: FocusRequester = remember { FocusRequester() },
    firstResultFocusRequester: FocusRequester? = null,
    onSearchSubmitted: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isEditing by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        readOnly = !isEditing,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            isEditing = false
            keyboardController?.hide()
            onSearchSubmitted()
            runCatching { firstResultFocusRequester?.requestFocus() }
        }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SirKTVBackground,
            unfocusedContainerColor = SirKTVBackground,
            focusedBorderColor = SirKTVPrimary,
            unfocusedBorderColor = SirKTVOnSurfaceMuted,
            cursorColor = SirKTVPrimary,
            focusedTextColor = SirKTVOnSurfaceStrong,
            unfocusedTextColor = SirKTVOnSurfaceStrong
        ),
        modifier = modifier
            .focusRequester(fieldFocusRequester)
            .onFocusChanged { state ->
                if (!state.isFocused) {
                    isEditing = false
                    keyboardController?.hide()
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (!isEditing) {
                            isEditing = true
                            keyboardController?.show()
                            true
                        } else false
                    }
                    Key.DirectionDown -> {
                        if (firstResultFocusRequester != null) {
                            isEditing = false
                            keyboardController?.hide()
                            runCatching { firstResultFocusRequester.requestFocus() }
                            true
                        } else false
                    }
                    else -> false
                }
            }
            .tvFocusStyle()
    )
}
