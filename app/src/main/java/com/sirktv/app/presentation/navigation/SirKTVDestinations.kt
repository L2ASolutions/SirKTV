package com.sirktv.app.presentation.navigation

object SirKTVDestinations {
    const val LOGIN = "login"
    const val HOME = "home"
    const val LIVE_TV_BROWSE = "live_tv_browse"
    const val LIVE_TV = "live_tv/{channelId}"
    const val SPORTS = "sports"
    const val SETTINGS = "settings"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val SERIES_DETAIL = "series_detail/{seriesId}"
    const val FAVORITES = "favorites"
    const val SEARCH = "search?query={query}"
    const val MOVIE_PLAYER = "movie_player/{movieId}"
    const val EPISODE_PLAYER = "episode_player/{seriesId}/{season}/{episode}"

    fun liveTv(channelId: String) = "live_tv/$channelId"
    fun seriesDetail(seriesId: String) = "series_detail/$seriesId"
    fun moviePlayer(movieId: String) = "movie_player/$movieId"
    fun episodePlayer(seriesId: String, season: Int, episode: Int) = "episode_player/$seriesId/$season/$episode"
    fun search(query: String = "") = "search?query=${java.net.URLEncoder.encode(query, "UTF-8")}"
}
