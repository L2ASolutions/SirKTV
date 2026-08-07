package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.SearchHistoryRepository
import javax.inject.Inject

class RecordSearchQueryUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) {
    suspend operator fun invoke(query: String) = searchHistoryRepository.record(query)
}
