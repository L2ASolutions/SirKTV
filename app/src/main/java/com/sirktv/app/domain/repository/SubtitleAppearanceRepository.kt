package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.SubtitleAppearance
import kotlinx.coroutines.flow.Flow

interface SubtitleAppearanceRepository {
    fun observe(): Flow<SubtitleAppearance>
    suspend fun update(settings: SubtitleAppearance)
}
