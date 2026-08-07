package com.sirktv.app.data.repository

import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.FavoriteItem
import com.sirktv.app.domain.model.FavoriteSort
import com.sirktv.app.domain.repository.FavoritesRepository
import com.sirktv.app.storage.db.FavoriteDao
import com.sirktv.app.storage.db.FavoriteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoritesRepository {

    override fun observe(contentType: ContentType, sort: FavoriteSort): Flow<List<FavoriteItem>> {
        val type = contentType.name
        val flow = when (sort) {
            FavoriteSort.ALPHABETICAL -> favoriteDao.observeAlphabetical(type)
            FavoriteSort.RECENTLY_ADDED -> favoriteDao.observeRecentlyAdded(type)
            FavoriteSort.CUSTOM -> favoriteDao.observeCustomOrder(type)
        }
        return flow.map { list -> list.map(::toDomain) }
    }

    override suspend fun moveUp(contentId: String, contentType: ContentType) = swap(contentId, contentType, -1)
    override suspend fun moveDown(contentId: String, contentType: ContentType) = swap(contentId, contentType, 1)

    private suspend fun swap(contentId: String, contentType: ContentType, direction: Int) {
        val type = contentType.name
        val ordered = favoriteDao.listCustomOrder(type)
        val index = ordered.indexOfFirst { it.contentId == contentId }
        val targetIndex = index + direction
        if (index < 0 || targetIndex < 0 || targetIndex >= ordered.size) return
        val current = ordered[index]
        val target = ordered[targetIndex]
        favoriteDao.updateSortOrder(current.contentId, type, target.sortOrder)
        favoriteDao.updateSortOrder(target.contentId, type, current.sortOrder)
    }

    private fun toDomain(entity: FavoriteEntity): FavoriteItem = FavoriteItem(
        contentId = entity.contentId,
        contentType = runCatching { ContentType.valueOf(entity.contentType) }.getOrDefault(ContentType.LIVE),
        title = entity.title,
        imageUrl = entity.imageUrl,
        sortOrder = entity.sortOrder,
        addedAtEpochMillis = entity.addedAtEpochMillis
    )
}
