package com.sirktv.app.domain.model

data class SearchResults(
    val channels: List<Channel>,
    val movies: List<Movie>,
    val series: List<Series>
) {
    val isEmpty: Boolean get() = channels.isEmpty() && movies.isEmpty() && series.isEmpty()

    companion object {
        val EMPTY = SearchResults(emptyList(), emptyList(), emptyList())
    }
}
