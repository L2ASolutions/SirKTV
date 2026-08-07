package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.FavoriteItem
import com.sirktv.app.domain.model.FavoriteSort
import com.sirktv.app.domain.repository.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAllFavoritesUseCase @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) {
    operator fun invoke(contentType: ContentType, sort: FavoriteSort): Flow<List<FavoriteItem>> =
        favoritesRepository.observe(contentType, sort)
}
