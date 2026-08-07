package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.SubtitleAppearance
import com.sirktv.app.domain.repository.SubtitleAppearanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSubtitleAppearanceUseCase @Inject constructor(
    private val subtitleAppearanceRepository: SubtitleAppearanceRepository
) {
    operator fun invoke(): Flow<SubtitleAppearance> = subtitleAppearanceRepository.observe()
}
