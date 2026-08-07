package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.repository.WatchProgressRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRecentlyWatchedUseCase @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository
) {
    operator fun invoke(): Flow<List<WatchProgress>> = watchProgressRepository.observeRecentlyWatched()
}
