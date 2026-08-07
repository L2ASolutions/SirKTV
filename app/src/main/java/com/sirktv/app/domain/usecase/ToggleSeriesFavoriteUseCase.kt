package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.SeriesRepository
import javax.inject.Inject

class ToggleSeriesFavoriteUseCase @Inject constructor(
    private val seriesRepository: SeriesRepository
) {
    suspend operator fun invoke(seriesId: String) = seriesRepository.toggleFavorite(seriesId)
}
