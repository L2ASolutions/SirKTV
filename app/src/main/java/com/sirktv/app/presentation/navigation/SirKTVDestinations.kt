package com.sirktv.app.presentation.navigation

import com.sirktv.app.presentation.settings.SettingsTile

object SirKTVDestinations {
    const val LOGIN = "login"
    const val HOME = "home"
    const val LIVE_TV_BROWSE = "live_tv_browse"
    const val LIVE_TV = "live_tv/{channelId}"
    const val SPORTS = "sports"
    const val SETTINGS = "settings"
    const val SETTINGS_TILE = "settings/{tileName}"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val SERIES_DETAIL = "series_detail/{seriesId}"
    const val FAVORITES = "favorites"
    const val SEARCH = "search"
    const val MOVIE_PLAYER = "movie_player/{movieId}"
    const val EPISODE_PLAYER = "episode_player/{seriesId}/{season}/{episode}"

    fun liveTv(channelId: String) = "live_tv/$channelId"
    fun settingsTile(tile: SettingsTile) = "settings/${tile.name}"
    fun seriesDetail(seriesId: String) = "series_detail/$seriesId"
    fun moviePlayer(movieId: String) = "movie_player/$movieId"
    fun episodePlayer(seriesId: String, season: Int, episode: Int) = "episode_player/$seriesId/$season/$episode"
}
