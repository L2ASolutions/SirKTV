package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.WatchProgressRepository
import javax.inject.Inject

class RemoveWatchProgressUseCase @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository
) {
    suspend operator fun invoke(contentId: String) = watchProgressRepository.remove(contentId)
}
