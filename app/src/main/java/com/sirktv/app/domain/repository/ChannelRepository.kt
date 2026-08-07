package com.sirktv.app.domain.repository

import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun observeCategories(): Flow<List<Category>>
    fun observeChannels(categoryId: String? = null): Flow<List<Channel>>
    fun observeFavoriteChannels(): Flow<List<Channel>>

    suspend fun getCachedChannels(): List<Channel>
    suspend fun getFavoriteChannelIds(): List<String>
    suspend fun toggleFavorite(channelId: String)

    /** Fetches categories + live streams from Xtream and refreshes the local cache. */
    suspend fun sync()

    /** Now/next for one channel, fetched on demand and short-lived — never persisted. */
    suspend fun getEpgNowNext(channelId: String): EpgNowNext

    /** Full upcoming listing for one channel, used by the program guide grid — never persisted. */
    suspend fun getEpgListings(channelId: String, limit: Int = 20): List<EpgProgram>
}
