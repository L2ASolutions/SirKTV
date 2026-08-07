package com.sirktv.app.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class NetworkTransport { WIFI, ETHERNET, CELLULAR, OTHER, NONE }

data class NetworkQualityInfo(val transport: NetworkTransport, val isLowBandWifi: Boolean)

/**
 * One-shot network snapshot used to power the "you're on 2.4GHz WiFi" quality
 * tip. Deliberately a poll, not a callback flow — it's only ever consulted at
 * the moment playback is already struggling, not watched continuously.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun currentInfo(): NetworkQualityInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkQualityInfo(NetworkTransport.NONE, isLowBandWifi = false)
        val network = connectivityManager.activeNetwork
            ?: return NetworkQualityInfo(NetworkTransport.NONE, isLowBandWifi = false)
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkQualityInfo(NetworkTransport.NONE, isLowBandWifi = false)

        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransport.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
            else -> NetworkTransport.OTHER
        }
        val isLowBandWifi = transport == NetworkTransport.WIFI && isOn24GhzBand()
        return NetworkQualityInfo(transport, isLowBandWifi)
    }

    @Suppress("DEPRECATION")
    private fun isOn24GhzBand(): Boolean = runCatching {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val frequencyMhz = wifiManager?.connectionInfo?.frequency ?: return@runCatching false
        frequencyMhz in 2400..2500
    }.getOrDefault(false)
}
