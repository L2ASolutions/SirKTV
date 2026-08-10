package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.repository.MovieRepository
import javax.inject.Inject

class GetCachedMoviesUseCase @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(): List<Movie> = movieRepository.getCachedMovies()
}
