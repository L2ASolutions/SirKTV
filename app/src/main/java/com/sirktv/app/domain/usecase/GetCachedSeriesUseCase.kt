package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.repository.SeriesRepository
import javax.inject.Inject

class GetCachedSeriesUseCase @Inject constructor(
    private val seriesRepository: SeriesRepository
) {
    suspend operator fun invoke(): List<Series> = seriesRepository.getCachedSeries()
}
