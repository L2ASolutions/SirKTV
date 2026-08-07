package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.MovieRepository
import javax.inject.Inject

class ToggleMovieFavoriteUseCase @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(movieId: String) = movieRepository.toggleFavorite(movieId)
}
