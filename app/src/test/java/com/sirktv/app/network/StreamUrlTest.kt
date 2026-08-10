package com.sirktv.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class StreamUrlTest {

    @Test
    fun `live stream URL format is correct m3u8 extension`() {
        val url = XtreamStreamUrlBuilder.buildPrimaryUrl("http://example.com:8080", "user", "pass", "123")
        assertEquals("http://example.com:8080/live/user/pass/123.m3u8", url)
    }

    @Test
    fun `live stream URL format is correct ts extension`() {
        val url = XtreamStreamUrlBuilder.buildBackupUrl("http://example.com:8080", "user", "pass", "123")
        assertEquals("http://example.com:8080/live/user/pass/123.ts", url)
    }

    @Test
    fun `movie URL uses the real container extension`() {
        val url = XtreamStreamUrlBuilder.buildMovieUrl("http://example.com", "user", "pass", "55", "mkv")
        assertEquals("http://example.com/movie/user/pass/55.mkv", url)
    }

    @Test
    fun `episode URL is addressed by episode id not series id`() {
        val url = XtreamStreamUrlBuilder.buildEpisodeUrl("http://example.com", "user", "pass", "77", "mp4")
        assertEquals("http://example.com/series/user/pass/77.mp4", url)
    }

    @Test
    fun `server address without scheme defaults to http`() {
        val url = XtreamStreamUrlBuilder.buildPrimaryUrl("example.com:8080", "user", "pass", "1")
        assertEquals("http://example.com:8080/live/user/pass/1.m3u8", url)
    }

    @Test
    fun `blank server address is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            XtreamStreamUrlBuilder.buildPrimaryUrl("   ", "user", "pass", "1")
        }
    }
}
