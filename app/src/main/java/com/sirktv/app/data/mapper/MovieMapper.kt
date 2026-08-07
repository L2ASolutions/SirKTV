package com.sirktv.app.data.mapper

import com.sirktv.app.domain.model.Category
import com.sirktv.app.domain.model.ContentType
import com.sirktv.app.domain.model.Movie
import com.sirktv.app.domain.model.MovieDetail
import com.sirktv.app.network.dto.VodInfoResponseDto
import com.sirktv.app.network.dto.VodStreamDto
import com.sirktv.app.network.dto.XtreamCategoryDto
import com.sirktv.app.storage.db.CategoryEntity
import com.sirktv.app.storage.db.MovieEntity
import com.sirktv.app.storage.db.MovieWithFavorite

internal object MovieMapper {

    fun toCategoryEntity(dto: XtreamCategoryDto): CategoryEntity? {
        val id = dto.categoryId ?: return null
        return CategoryEntity(id = id, name = dto.categoryName ?: id, contentType = ContentType.MOVIE.name)
    }

    fun toMovieEntity(dto: VodStreamDto, nowEpochMillis: Long): MovieEntity? {
        val id = dto.streamId?.toString() ?: return null
        val categoryId = dto.categoryId ?: return null
        return MovieEntity(
            id = id,
            title = dto.name ?: "Untitled",
            posterUrl = dto.streamIcon?.takeIf { it.isNotBlank() },
            categoryId = categoryId,
            rating = dto.rating?.toFloatOrNull(),
            containerExtension = dto.containerExtension?.takeIf { it.isNotBlank() } ?: "mp4",
            addedAtEpochMillis = nowEpochMillis
        )
    }

    fun toCategory(entity: CategoryEntity): Category = Category(id = entity.id, name = entity.name)

    fun toMovie(row: MovieWithFavorite): Movie = Movie(
        id = row.id,
        title = row.title,
        posterUrl = row.posterUrl,
        categoryId = row.categoryId,
        rating = row.rating,
        containerExtension = row.containerExtension,
        isFavorite = row.isFavorite
    )

    fun toMovieDetail(dto: VodInfoResponseDto): MovieDetail? {
        val info = dto.info ?: return null
        val castText = info.cast?.takeIf { it.isNotBlank() } ?: info.actors
        val cast = castText?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        val synopsis = info.plot?.takeIf { it.isNotBlank() } ?: info.description
        val durationMinutes = info.durationSecs?.let { it / 60 } ?: parseDurationToMinutes(info.duration)
        return MovieDetail(
            synopsis = synopsis,
            cast = cast,
            director = info.director?.takeIf { it.isNotBlank() },
            durationMinutes = durationMinutes,
            genre = info.genre?.takeIf { it.isNotBlank() }
        )
    }

    /** Xtream sometimes returns duration as "HH:MM:SS" instead of duration_secs. */
    private fun parseDurationToMinutes(duration: String?): Int? {
        if (duration.isNullOrBlank()) return null
        val parts = duration.split(":").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 60 + parts[1]
            2 -> parts[0]
            else -> null
        }
    }
}
