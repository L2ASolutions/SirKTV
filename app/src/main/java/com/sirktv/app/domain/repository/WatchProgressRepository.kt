package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.WatchProgress
import kotlinx.coroutines.flow.Flow

interface WatchProgressRepository {
    /** In-progress, non-live content only — the Home/Movies/Series "Continue Watching" rows. */
    fun observeContinueWatching(): Flow<List<WatchProgress>>

    /** Everything, including fully-watched and live channels — the Home "Recently Watched" row. */
    fun observeRecentlyWatched(): Flow<List<WatchProgress>>

    suspend fun getProgress(contentId: String): WatchProgress?
    suspend fun upsert(progress: WatchProgress)
    suspend fun remove(contentId: String)
    suspend fun clearAll()
}
