package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMovieCategoriesUseCase @Inject constructor(
    private val movieRepository: MovieRepository
) {
    operator fun invoke(): Flow<List<Category>> = movieRepository.observeCategories()
}
