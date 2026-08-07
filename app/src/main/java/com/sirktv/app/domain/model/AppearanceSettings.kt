package com.sirktv.app.domain.model

enum class TextScale { SMALL, MEDIUM, LARGE }
enum class FocusStyle { GLOW, OUTLINE, SCALE_ONLY }

data class AppearanceSettings(
    val textScale: TextScale = TextScale.MEDIUM,
    val focusStyle: FocusStyle = FocusStyle.GLOW
)
