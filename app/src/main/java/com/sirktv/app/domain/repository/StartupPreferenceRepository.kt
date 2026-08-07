package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.StartupPreference
import kotlinx.coroutines.flow.Flow

interface StartupPreferenceRepository {
    fun observe(): Flow<StartupPreference>
    suspend fun get(): StartupPreference
    suspend fun update(preference: StartupPreference)
    suspend fun setLastWatchedChannel(channelId: String)
}
