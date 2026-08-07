package com.sirktv.app.domain.model

/** [pinHash] null means parental controls are off entirely — no PIN configured. */
data class ParentalSettings(
    val pinHash: String? = null,
    val lockedCategoryIds: Set<String> = emptySet()
) {
    val isEnabled: Boolean get() = pinHash != null
}
