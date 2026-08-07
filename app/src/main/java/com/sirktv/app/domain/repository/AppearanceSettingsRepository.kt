package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.AppearanceSettings
import kotlinx.coroutines.flow.Flow

interface AppearanceSettingsRepository {
    fun observe(): Flow<AppearanceSettings>
    suspend fun update(settings: AppearanceSettings)
}
