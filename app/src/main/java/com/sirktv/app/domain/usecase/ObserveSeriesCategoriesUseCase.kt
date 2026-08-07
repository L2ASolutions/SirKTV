package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.repository.SeriesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSeriesCategoriesUseCase @Inject constructor(
    private val seriesRepository: SeriesRepository
) {
    operator fun invoke(): Flow<List<Category>> = seriesRepository.observeCategories()
}
