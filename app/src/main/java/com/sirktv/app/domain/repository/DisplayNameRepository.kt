package com.sirktv.app.domain.repository

import kotlinx.coroutines.flow.Flow

/** The user-chosen greeting name set on Login ("How should we greet you?"). Null/blank means fall back to username. */
interface DisplayNameRepository {
    fun observe(): Flow<String?>
    suspend fun get(): String?
    suspend fun set(displayName: String?)
}
