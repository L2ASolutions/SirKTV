package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.SearchHistoryRepository
import javax.inject.Inject

class ClearSearchHistoryUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) {
    suspend operator fun invoke() = searchHistoryRepository.clear()
}
