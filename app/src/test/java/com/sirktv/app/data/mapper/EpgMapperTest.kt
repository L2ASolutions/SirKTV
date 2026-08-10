package com.sirktv.app.data.mapper

import com.sirktv.app.network.dto.EpgListingDto
import com.sirktv.app.network.dto.EpgResponseDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Titles/descriptions are left blank in these fixtures on purpose:
 * EpgMapper.decodeBase64 short-circuits to null before ever touching
 * android.util.Base64 when the input is blank, and android.util.Base64 is an
 * unimplemented stub outside a real device/Robolectric — so blank text here
 * is what keeps this a pure, fast JVM unit test of the actual thing under
 * test: which listing gets picked as "now" for a given clock time.
 */
class EpgMapperTest {

    private fun listing(startEpochSeconds: Long, endEpochSeconds: Long) = EpgListingDto(
        title = "",
        description = "",
        startTimestamp = startEpochSeconds.toString(),
        stopTimestamp = endEpochSeconds.toString()
    )

    @Test
    fun `EPG current program returns correct listing for now`() {
        val nowSeconds = System.currentTimeMillis() / 1000
        val current = listing(startEpochSeconds = nowSeconds - 600, endEpochSeconds = nowSeconds + 600)
        val past = listing(startEpochSeconds = nowSeconds - 3600, endEpochSeconds = nowSeconds - 1800)
        val future = listing(startEpochSeconds = nowSeconds + 1800, endEpochSeconds = nowSeconds + 3600)

        val nowNext = EpgMapper.toEpgNowNext(EpgResponseDto(epgListings = listOf(past, future, current)))

        assertEquals(current.startTimestamp!!.toLong(), nowNext.now?.startEpochSeconds)
        assertEquals(future.startTimestamp!!.toLong(), nowNext.next?.startEpochSeconds)
    }

    @Test
    fun `no currently airing listing falls back to earliest available`() {
        val nowSeconds = System.currentTimeMillis() / 1000
        val past = listing(startEpochSeconds = nowSeconds - 7200, endEpochSeconds = nowSeconds - 3600)
        val future = listing(startEpochSeconds = nowSeconds + 3600, endEpochSeconds = nowSeconds + 7200)

        val nowNext = EpgMapper.toEpgNowNext(EpgResponseDto(epgListings = listOf(future, past)))

        // No listing spans "now" — falls back to the earliest one in the list.
        assertEquals(past.startTimestamp!!.toLong(), nowNext.now?.startEpochSeconds)
    }
}
