package com.sirktv.app.data.repository

import com.sirktv.app.data.mapper.ChannelMapper
import com.sirktv.app.data.mapper.EpgMapper
import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.domain.repository.ChannelRepository
import com.sirktv.app.domain.session.CurrentSession
import com.sirktv.app.network.XtreamContentApiService
import com.sirktv.app.network.XtreamUrlBuilder
import com.sirktv.app.storage.db.ChannelDao
import com.sirktv.app.storage.db.FavoriteDao
import com.sirktv.app.storage.db.FavoriteEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChannelRepositoryImpl @Inject constructor(
    private val apiService: XtreamContentApiService,
    private val channelDao: ChannelDao,
    private val favoriteDao: FavoriteDao,
    private val currentSession: CurrentSession
) : ChannelRepository {

    private val contentType = ContentType.LIVE.name

    override fun observeCategories(): Flow<List<Category>> =
        channelDao.observeCategories().map { list -> list.map(ChannelMapper::toCategory) }

    override fun observeChannels(categoryId: String?): Flow<List<Channel>> =
        channelDao.observeChannels(categoryId).map { list -> list.map(ChannelMapper::toChannel) }

    override fun observeFavoriteChannels(): Flow<List<Channel>> =
        channelDao.observeFavoriteChannels().map { list -> list.map(ChannelMapper::toChannel) }

    override suspend fun getCachedChannels(): List<Channel> {
        val favoriteIds = channelDao.getFavoriteChannelIds().toSet()
        return channelDao.getCachedChannels().map { ChannelMapper.toChannel(it, it.id in favoriteIds) }
    }

    override suspend fun getFavoriteChannelIds(): List<String> = channelDao.getFavoriteChannelIds()

    override suspend fun toggleFavorite(channelId: String) {
        if (favoriteDao.isFavorite(channelId, contentType)) {
            favoriteDao.delete(channelId, contentType)
        } else {
            val name = channelDao.getChannelName(channelId) ?: channelId
            val sortOrder = favoriteDao.maxSortOrder(contentType) + 1
            favoriteDao.insert(
                FavoriteEntity(
                    contentId = channelId,
                    contentType = contentType,
                    title = name,
                    imageUrl = null,
                    sortOrder = sortOrder,
                    addedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun sync(): Result<Unit> {
        val credentials = currentSession.credentials.value
            ?: return Result.failure(IllegalStateException("Not signed in"))
        // On failure (network, malformed URL, unexpected payload shape) the
        // existing cache is left untouched — a stale channel list beats a
        // broken one — but the caller still needs to know sync failed so an
        // on-demand loading screen can show an error instead of hanging.
        return runCatching {
            val playerApiUrl = XtreamUrlBuilder.buildPlayerApiUrl(credentials.serverUrl)
            coroutineScope {
                val categoriesDto = async { apiService.getLiveCategories(playerApiUrl, credentials.username, credentials.password) }
                val streamsDto = async { apiService.getLiveStreams(playerApiUrl, credentials.username, credentials.password) }
                val categories = categoriesDto.await().mapNotNull(ChannelMapper::toCategoryEntity)
                val channels = streamsDto.await().mapNotNull(ChannelMapper::toChannelEntity)
                channelDao.replaceAll(categories, channels)
            }
        }
    }

    override suspend fun getEpgNowNext(channelId: String): EpgNowNext {
        val credentials = currentSession.credentials.value ?: return EpgNowNext(now = null, next = null)
        val playerApiUrl = runCatching { XtreamUrlBuilder.buildPlayerApiUrl(credentials.serverUrl) }
            .getOrElse { return EpgNowNext(now = null, next = null) }
        return runCatching {
            val response = apiService.getShortEpg(playerApiUrl, credentials.username, credentials.password, channelId)
            EpgMapper.toEpgNowNext(response)
        }.getOrDefault(EpgNowNext(now = null, next = null))
    }

    /**
     * get_simple_data_table (the "full" EPG) is tried first so the schedule
     * panel shows a whole day of programming instead of get_short_epg's
     * narrow window — falls back to get_short_epg whenever the full endpoint
     * fails or a panel returns nothing for it, so the schedule is never empty
     * just because the richer endpoint isn't supported.
     */
    override suspend fun getEpgListings(channelId: String, limit: Int): List<EpgProgram> {
        val credentials = currentSession.credentials.value ?: return emptyList()
        val playerApiUrl = runCatching { XtreamUrlBuilder.buildPlayerApiUrl(credentials.serverUrl) }
            .getOrElse { return emptyList() }

        val fullEpg = runCatching {
            apiService.getFullEpg(playerApiUrl, credentials.username, credentials.password, channelId)
        }.getOrNull()?.let(EpgMapper::toEpgPrograms).orEmpty()
        if (fullEpg.isNotEmpty()) return fullEpg

        return runCatching {
            val response = apiService.getShortEpg(playerApiUrl, credentials.username, credentials.password, channelId, limit)
            EpgMapper.toEpgPrograms(response)
        }.getOrDefault(emptyList())
    }
}
