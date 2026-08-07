package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.repository.SeriesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSeriesUseCase @Inject constructor(
    private val seriesRepository: SeriesRepository
) {
    operator fun invoke(categoryId: String? = null): Flow<List<Series>> = seriesRepository.observeSeries(categoryId)
}
