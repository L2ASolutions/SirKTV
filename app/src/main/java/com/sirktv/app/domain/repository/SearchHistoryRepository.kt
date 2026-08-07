package com.sirktv.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SearchHistoryRepository {
    fun observeRecent(limit: Int = 8): Flow<List<String>>
    suspend fun record(query: String)
    suspend fun clear()
}
