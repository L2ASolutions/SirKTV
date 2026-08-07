package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.MovieDetail
import com.sirktv.app.domain.repository.MovieRepository
import javax.inject.Inject

class GetMovieDetailUseCase @Inject constructor(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(movieId: String): MovieDetail? = movieRepository.getMovieDetail(movieId)
}
