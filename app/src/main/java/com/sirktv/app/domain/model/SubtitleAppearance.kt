package com.sirktv.app.domain.model

enum class SubtitleTextSize(val fraction: Float) {
    SMALL(0.033f), MEDIUM(0.0533f), LARGE(0.075f), EXTRA_LARGE(0.10f)
}

enum class SubtitleTextColor { WHITE, YELLOW, GREEN, CYAN }

enum class SubtitleBackground { NONE, SEMI_TRANSPARENT, SOLID }

enum class SubtitleEdgeStyle { NONE, OUTLINE, DROP_SHADOW }

data class SubtitleAppearance(
    val textSize: SubtitleTextSize = SubtitleTextSize.MEDIUM,
    val textColor: SubtitleTextColor = SubtitleTextColor.WHITE,
    val background: SubtitleBackground = SubtitleBackground.SEMI_TRANSPARENT,
    val edgeStyle: SubtitleEdgeStyle = SubtitleEdgeStyle.OUTLINE
)
