package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.repository.MovieRepository
import javax.inject.Inject

class SyncMoviesUseCase @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke() = movieRepository.sync()
}
