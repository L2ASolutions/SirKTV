package com.sirktv.app.domain.usecase

import com.sirktv.app.domain.model.SearchResults
import com.sirktv.app.domain.repository.ChannelRepository
import com.sirktv.app.domain.repository.MovieRepository
import com.sirktv.app.domain.repository.SeriesRepository
import javax.inject.Inject

/**
 * Searches the already-synced local catalogs (channels/movies/series) — no
 * network round-trip per keystroke. Requires 2+ characters, matching the brief.
 */
class SearchContentUseCase @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val movieRepository: MovieRepository,
    private val seriesRepository: SeriesRepository
) {
    suspend operator fun invoke(query: String): SearchResults {
        val normalized = query.trim().lowercase()
        if (normalized.length < 2) return SearchResults.EMPTY

        val channels = channelRepository.getCachedChannels().filter { it.name.lowercase().contains(normalized) }
        val movies = movieRepository.getCachedMovies().filter { it.title.lowercase().contains(normalized) }
        val series = seriesRepository.getCachedSeries().filter { it.title.lowercase().contains(normalized) }

        return SearchResults(channels, movies, series)
    }
}
