package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.SeriesInfo
import com.sirktv.app.domain.repository.SeriesRepository
import javax.inject.Inject

class GetSeriesInfoUseCase @Inject constructor(
    private val seriesRepository: SeriesRepository
) {
    suspend operator fun invoke(seriesId: String): SeriesInfo? = seriesRepository.getSeriesInfo(seriesId)
}
