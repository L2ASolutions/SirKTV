package com.sirktv.app.data.mapper

import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.Channel
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.network.dto.LiveCategoryDto
import com.sirktv.app.network.dto.LiveStreamDto
import com.sirktv.app.storage.db.CategoryEntity
import com.sirktv.app.storage.db.ChannelEntity
import com.sirktv.app.storage.db.ChannelWithFavorite

internal object ChannelMapper {

    fun toCategoryEntity(dto: LiveCategoryDto): CategoryEntity? {
        val id = dto.categoryId ?: return null
        return CategoryEntity(id = id, name = dto.categoryName ?: id, contentType = ContentType.LIVE.name)
    }

    fun toChannelEntity(dto: LiveStreamDto): ChannelEntity? {
        val id = dto.streamId?.toString() ?: return null
        val categoryId = dto.categoryId ?: return null
        return ChannelEntity(
            id = id,
            name = dto.name ?: "Channel $id",
            logoUrl = dto.streamIcon?.takeIf { it.isNotBlank() },
            categoryId = categoryId,
            channelNumber = dto.num ?: 0
        )
    }

    fun toCategory(entity: CategoryEntity): Category = Category(id = entity.id, name = entity.name)

    fun toChannel(row: ChannelWithFavorite): Channel = Channel(
        id = row.id,
        name = row.name,
        logoUrl = row.logoUrl,
        categoryId = row.categoryId,
        channelNumber = row.channelNumber,
        isFavorite = row.isFavorite
    )

    fun toChannel(entity: ChannelEntity, isFavorite: Boolean): Channel = Channel(
        id = entity.id,
        name = entity.name,
        logoUrl = entity.logoUrl,
        categoryId = entity.categoryId,
        channelNumber = entity.channelNumber,
        isFavorite = isFavorite
    )
}
