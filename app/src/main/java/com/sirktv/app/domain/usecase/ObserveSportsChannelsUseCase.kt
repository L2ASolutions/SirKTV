package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.SportsCatalog
import com.sirktv.app.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Auto-detects "Sports" categories by keyword match against the account's own
 * category names — every Xtream provider names categories differently, so a
 * curated per-provider mapping would need constant upkeep; keyword matching
 * works identically on day one for any provider.
 */
class ObserveSportsChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    operator fun invoke(): Flow<SportsCatalog> = combine(
        channelRepository.observeCategories(),
        channelRepository.observeChannels(categoryId = null)
    ) { categories, channels ->
        val sportsCategories = categories.filter { isSportsCategory(it.name) }
        val sportsCategoryIds = sportsCategories.map { it.id }.toSet()
        SportsCatalog(
            categories = sportsCategories,
            channels = channels.filter { it.categoryId in sportsCategoryIds }
        )
    }

    private fun isSportsCategory(name: String): Boolean {
        val normalized = name.lowercase()
        return SPORTS_KEYWORDS.any { normalized.contains(it) }
    }

    private companion object {
        val SPORTS_KEYWORDS = listOf(
            "sport", "sports", "football", "soccer", "nba", "nfl", "ufc",
            "boxing", "cricket", "tennis", "f1", "racing", "motor"
        )
    }
}
