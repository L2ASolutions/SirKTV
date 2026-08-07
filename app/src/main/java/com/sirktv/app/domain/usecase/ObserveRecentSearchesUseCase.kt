package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRecentSearchesUseCase @Inject constructor(
    private val searchHistoryRepository: SearchHistoryRepository
) {
    operator fun invoke(limit: Int = 8): Flow<List<String>> = searchHistoryRepository.observeRecent(limit)
}
