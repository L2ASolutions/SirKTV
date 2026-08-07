package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.repository.WatchProgressRepository
import javax.inject.Inject

class GetWatchProgressUseCase @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository
) {
    suspend operator fun invoke(contentId: String): WatchProgress? = watchProgressRepository.getProgress(contentId)
}
