package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFavoriteMoviesUseCase @Inject constructor(
    private val movieRepository: MovieRepository
) {
    operator fun invoke(): Flow<List<Movie>> = movieRepository.observeFavoriteMovies()
}
