package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.SeriesRepository
import javax.inject.Inject

class SyncSeriesUseCase @Inject constructor(
    private val seriesRepository: SeriesRepository
) {
    suspend operator fun invoke() = seriesRepository.sync()
}
