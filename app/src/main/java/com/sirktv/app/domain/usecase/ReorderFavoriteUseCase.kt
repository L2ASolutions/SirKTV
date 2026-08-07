package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.repository.FavoritesRepository
import javax.inject.Inject

class ReorderFavoriteUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) {
    suspend fun moveUp(contentId: String, contentType: ContentType) = favoritesRepository.moveUp(contentId, contentType)
    suspend fun moveDown(contentId: String, contentType: ContentType) = favoritesRepository.moveDown(contentId, contentType)
}
