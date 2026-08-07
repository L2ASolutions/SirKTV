package com.sirktv.app.data.mapper

import android.util.Base64
import com.sirktv.app.domain.model.EpgNowNext
import com.sirktv.app.domain.model.EpgProgram
import com.sirktv.app.network.dto.EpgListingDto
import com.sirktv.app.network.dto.EpgResponseDto

internal object EpgMapper {

    fun toEpgNowNext(dto: EpgResponseDto): EpgNowNext {
        val listings = toEpgPrograms(dto)
        val nowSeconds = System.currentTimeMillis() / 1000
        val now = listings.firstOrNull { nowSeconds in it.startEpochSeconds until it.endEpochSeconds }
        val next = listings.firstOrNull { it.startEpochSeconds > (now?.endEpochSeconds ?: nowSeconds) }
        return EpgNowNext(now = now ?: listings.firstOrNull(), next = next)
    }

    /** Full upcoming listing (not reduced to now/next) — backs the program guide grid's timeline. */
    fun toEpgPrograms(dto: EpgResponseDto): List<EpgProgram> =
        dto.epgListings.orEmpty().mapNotNull(::toEpgProgram).sortedBy { it.startEpochSeconds }

    private fun toEpgProgram(dto: EpgListingDto): EpgProgram? {
        val start = dto.startTimestamp?.toLongOrNull() ?: return null
        val end = dto.stopTimestamp?.toLongOrNull() ?: return null
        return EpgProgram(
            title = decodeBase64(dto.title) ?: "Untitled",
            description = decodeBase64(dto.description).orEmpty(),
            startEpochSeconds = start,
            endEpochSeconds = end
        )
    }

    private fun decodeBase64(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching { String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8) }.getOrNull()
    }
}
