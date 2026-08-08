package com.sirktv.app.presentation.screensaver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.domain.session.PlayerPresence
import com.sirktv.app.domain.usecase.ObserveMovieCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveMoviesUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesCategoriesUseCase
import com.sirktv.app.domain.usecase.ObserveSeriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val SCREENSAVER_ITEM_LIMIT = 10

private val MATURE_KEYWORDS = listOf("xxx", "adult", "18+", "porn", "erotic")

private fun isMatureCategory(name: String): Boolean {
    val normalized = name.lowercase()
    return MATURE_KEYWORDS.any { normalized.contains(it) }
}

/** One rotating slide in the idle screensaver. */
data class ScreensaverItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val backdropUrl: String?
)

@HiltViewModel
class ScreensaverViewModel @Inject constructor(
    observeMoviesUseCase: ObserveMoviesUseCase,
    observeSeriesUseCase: ObserveSeriesUseCase,
    observeMovieCategoriesUseCase: ObserveMovieCategoriesUseCase,
    observeSeriesCategoriesUseCase: ObserveSeriesCategoriesUseCase,
    currentSession: CurrentSession,
    playerPresence: PlayerPresence
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = currentSession.profile
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isPlayerActive: StateFlow<Boolean> = playerPresence.isPlayerActive

    val items: StateFlow<List<ScreensaverItem>> = combine(
        observeMoviesUseCase(),
        observeSeriesUseCase(),
        observeMovieCategoriesUseCase(),
        observeSeriesCategoriesUseCase()
    ) { movies, series, movieCategories, seriesCategories ->
        buildItems(movies, series, movieCategories, seriesCategories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun buildItems(
        movies: List<Movie>,
        series: List<Series>,
        movieCategories: List<Category>,
        seriesCategories: List<Category>
    ): List<ScreensaverItem> {
        val matureMovieCategoryIds = movieCategories.filter { isMatureCategory(it.name) }.map { it.id }.toSet()
        val matureSeriesCategoryIds = seriesCategories.filter { isMatureCategory(it.name) }.map { it.id }.toSet()

        val movieItems = movies
            .filterNot { it.categoryId in matureMovieCategoryIds }
            .sortedByDescending { it.addedAtEpochMillis }
            .take(SCREENSAVER_ITEM_LIMIT)
            .map { movie ->
                ScreensaverItem(
                    id = "movie-${movie.id}",
                    title = movie.title,
                    subtitle = movie.rating?.let { "★ ${"%.1f".format(it)}" },
                    backdropUrl = movie.posterUrl
                )
            }
        // Series has no add-time column in the local schema, so it's ranked by
        // catalog order rather than a fabricated timestamp — same tradeoff as
        // Home's and Series' "Recently Added" rows.
        val seriesItems = series
            .filterNot { it.categoryId in matureSeriesCategoryIds }
            .take(SCREENSAVER_ITEM_LIMIT)
            .map { s ->
                ScreensaverItem(
                    id = "series-${s.id}",
                    title = s.title,
                    subtitle = s.rating?.let { "★ ${"%.1f".format(it)}" },
                    backdropUrl = s.posterUrl
                )
            }

        return (movieItems + seriesItems).take(SCREENSAVER_ITEM_LIMIT)
    }
}
