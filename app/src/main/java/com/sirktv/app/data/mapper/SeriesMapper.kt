package com.sirktv.app.data.mapper

import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.Episode
import com.sirktv.app.domain.model.SeasonInfo
import com.sirktv.app.domain.model.Series
import com.sirktv.app.domain.model.SeriesInfo
import com.sirktv.app.network.dto.SeriesDto
import com.sirktv.app.network.dto.SeriesInfoResponseDto
import com.sirktv.app.network.dto.XtreamCategoryDto
import com.sirktv.app.storage.db.CategoryEntity
import com.sirktv.app.storage.db.EpisodeEntity
import com.sirktv.app.storage.db.SeriesEntity
import com.sirktv.app.storage.db.SeriesWithFavorite

internal object SeriesMapper {

    fun toCategoryEntity(dto: XtreamCategoryDto): CategoryEntity? {
        val id = dto.categoryId ?: return null
        return CategoryEntity(id = id, name = dto.categoryName ?: id, contentType = ContentType.SERIES.name)
    }

    fun toSeriesEntity(dto: SeriesDto): SeriesEntity? {
        val id = dto.seriesId?.toString() ?: return null
        val categoryId = dto.categoryId ?: return null
        return SeriesEntity(
            id = id,
            title = dto.name ?: "Untitled",
            posterUrl = dto.cover?.takeIf { it.isNotBlank() },
            categoryId = categoryId,
            rating = dto.rating?.toFloatOrNull(),
            cast = dto.cast?.takeIf { it.isNotBlank() },
            director = dto.director?.takeIf { it.isNotBlank() },
            synopsis = dto.plot?.takeIf { it.isNotBlank() },
            lastModifiedEpochMillis = dto.lastModified?.toLongOrNull()?.takeIf { it > 0 }?.times(1000) ?: 0L
        )
    }

    fun toCategory(entity: CategoryEntity): Category = Category(id = entity.id, name = entity.name)

    fun toSeries(row: SeriesWithFavorite): Series = Series(
        id = row.id,
        title = row.title,
        posterUrl = row.posterUrl,
        categoryId = row.categoryId,
        rating = row.rating,
        cast = row.cast?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
        director = row.director,
        synopsis = row.synopsis,
        lastModifiedEpochMillis = row.lastModifiedEpochMillis,
        isFavorite = row.isFavorite
    )

    /** Flattened for [com.sirktv.app.storage.db.EpisodeDao.insertAll] — the network shape (grouped by season key) isn't what Room stores. */
    fun toEpisodeEntities(seriesId: String, dto: SeriesInfoResponseDto): List<EpisodeEntity> =
        dto.episodes.orEmpty().flatMap { (seasonKey, episodeDtos) ->
            val seasonNumber = seasonKey.toIntOrNull() ?: return@flatMap emptyList()
            episodeDtos.mapNotNull { episodeDto ->
                val episodeId = episodeDto.id ?: return@mapNotNull null
                val episodeNumber = episodeDto.episodeNum ?: return@mapNotNull null
                EpisodeEntity(
                    id = episodeId,
                    seriesId = seriesId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    title = episodeDto.title?.takeIf { it.isNotBlank() } ?: "Episode $episodeNumber",
                    durationMinutes = episodeDto.info?.durationSecs?.let { it / 60 },
                    containerExtension = episodeDto.containerExtension?.takeIf { it.isNotBlank() } ?: "mp4",
                    synopsis = episodeDto.info?.plot?.takeIf { it.isNotBlank() }
                )
            }
        }

    /** Regroups the flat Room rows back into the season/episode shape [SeriesDetailScreen] renders — same grouping [toEpisodeEntities] flattens away. */
    fun toSeriesInfo(entities: List<EpisodeEntity>): SeriesInfo {
        val seasons = entities
            .groupBy { it.seasonNumber }
            .map { (seasonNumber, episodeEntities) ->
                SeasonInfo(seasonNumber, episodeEntities.sortedBy { it.episodeNumber }.map(::toEpisode))
            }
            .sortedBy { it.seasonNumber }
        return SeriesInfo(seasons)
    }

    private fun toEpisode(entity: EpisodeEntity): Episode = Episode(
        id = entity.id,
        seriesId = entity.seriesId,
        seasonNumber = entity.seasonNumber,
        episodeNumber = entity.episodeNumber,
        title = entity.title,
        durationMinutes = entity.durationMinutes,
        containerExtension = entity.containerExtension,
        synopsis = entity.synopsis
    )
}
