package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.ParentalSettings
import kotlinx.coroutines.flow.Flow

interface ParentalSettingsRepository {
    fun observe(): Flow<ParentalSettings>
    suspend fun get(): ParentalSettings
    suspend fun setPin(pin: String?)
    suspend fun setCategoryLocked(categoryId: String, locked: Boolean)
    suspend fun verifyPin(pin: String): Boolean
}
