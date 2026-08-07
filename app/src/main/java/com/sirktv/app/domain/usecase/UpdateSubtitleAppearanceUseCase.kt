package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.SubtitleAppearance
import com.sirktv.app.domain.repository.SubtitleAppearanceRepository
import javax.inject.Inject

class UpdateSubtitleAppearanceUseCase @Inject constructor(
    private val subtitleAppearanceRepository: SubtitleAppearanceRepository
) {
    suspend operator fun invoke(settings: SubtitleAppearance) = subtitleAppearanceRepository.update(settings)
}
