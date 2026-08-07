package com.sirktv.app.data.repository

import com.sirktv.app.domain.repository.SearchHistoryRepository
import com.sirktv.app.storage.db.SearchHistoryDao
import com.sirktv.app.storage.db.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchHistoryRepositoryImpl @Inject constructor(
    private val dao: SearchHistoryDao
) : SearchHistoryRepository {

    override fun observeRecent(limit: Int): Flow<List<String>> = dao.observeRecent(limit)

    override suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        dao.record(SearchHistoryEntity(query = trimmed, searchedAtEpochMillis = System.currentTimeMillis()))
    }

    override suspend fun clear() = dao.clear()
}
