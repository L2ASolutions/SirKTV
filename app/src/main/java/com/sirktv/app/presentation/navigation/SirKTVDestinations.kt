package com.sirktv.app.presentation.navigation

import com.sirktv.app.presentation.settings.SettingsTile

object SirKTVDestinations {
    const val LOGIN = "login"
    const val HUB = "hub"
    const val LIVE_TV = "live_tv/{channelId}"
    const val SPORTS = "sports"
    const val SETTINGS = "settings"
    const val SETTINGS_TILE = "settings/{tileName}"

    fun liveTv(channelId: String) = "live_tv/$channelId"
    fun settingsTile(tile: SettingsTile) = "settings/${tile.name}"
}
