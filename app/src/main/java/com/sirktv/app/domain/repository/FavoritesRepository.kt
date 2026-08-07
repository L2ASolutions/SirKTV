package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.FavoriteItem
import com.sirktv.app.domain.model.FavoriteSort
import kotlinx.coroutines.flow.Flow

/**
 * Cross-type view over favorites for the dedicated Favorites screen. Adding/
 * removing a favorite still goes through each content type's own repository
 * (ChannelRepository.toggleFavorite, MovieRepository.toggleFavorite, ...) —
 * this repository only reads across types and handles manual reordering.
 */
interface FavoritesRepository {
    fun observe(contentType: ContentType, sort: FavoriteSort): Flow<List<FavoriteItem>>
    suspend fun moveUp(contentId: String, contentType: ContentType)
    suspend fun moveDown(contentId: String, contentType: ContentType)
}
