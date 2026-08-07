package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.repository.WatchProgressRepository
import javax.inject.Inject

class SaveWatchProgressUseCase @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository
) {
    suspend operator fun invoke(progress: WatchProgress) = watchProgressRepository.upsert(progress)
}
