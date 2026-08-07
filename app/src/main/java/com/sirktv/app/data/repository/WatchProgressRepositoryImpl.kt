package com.sirktv.app.data.repository

import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.WatchProgress
import com.sirktv.app.domain.repository.WatchProgressRepository
import com.sirktv.app.storage.db.WatchProgressDao
import com.sirktv.app.storage.db.WatchProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WatchProgressRepositoryImpl @Inject constructor(
    private val dao: WatchProgressDao
) : WatchProgressRepository {

    override fun observeContinueWatching(): Flow<List<WatchProgress>> =
        dao.observeContinueWatching().map { list -> list.map(::toDomain) }

    override fun observeRecentlyWatched(): Flow<List<WatchProgress>> =
        dao.observeRecentlyWatched().map { list -> list.map(::toDomain) }

    override suspend fun getProgress(contentId: String): WatchProgress? = dao.get(contentId)?.let(::toDomain)

    override suspend fun upsert(progress: WatchProgress) = dao.upsert(
        WatchProgressEntity(
            contentId = progress.contentId,
            contentType = progress.contentType.name,
            title = progress.title,
            subtitle = progress.subtitle,
            imageUrl = progress.imageUrl,
            positionMs = progress.positionMs,
            durationMs = progress.durationMs,
            updatedAtEpochMillis = progress.updatedAtEpochMillis
        )
    )

    override suspend fun remove(contentId: String) = dao.remove(contentId)
    override suspend fun clearAll() = dao.clearAll()

    private fun toDomain(entity: WatchProgressEntity): WatchProgress = WatchProgress(
        contentId = entity.contentId,
        contentType = runCatching { ContentType.valueOf(entity.contentType) }.getOrDefault(ContentType.LIVE),
        title = entity.title,
        subtitle = entity.subtitle,
        imageUrl = entity.imageUrl,
        positionMs = entity.positionMs,
        durationMs = entity.durationMs,
        updatedAtEpochMillis = entity.updatedAtEpochMillis
    )
}
