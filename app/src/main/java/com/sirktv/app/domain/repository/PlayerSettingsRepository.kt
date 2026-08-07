package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.PlayerSettings
import kotlinx.coroutines.flow.Flow

interface PlayerSettingsRepository {
    fun observe(): Flow<PlayerSettings>
    suspend fun update(settings: PlayerSettings)
}
