package com.flixclusive.data.downloads.util

import com.flixclusive.model.media.Movie
import com.flixclusive.model.media.common.tv.Episode
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DownloadPathUtilTest {
    private val testMovie = Movie(
        id = "123",
        title = "Everything Everywhere: All at Once?",
        providerId = "test-provider",
        posterImage = null,
    )

    private val testEpisode = Episode(
        id = "ep-1",
        number = 1,
        season = 1,
        isReleased = true,
        title = "Pilot",
    )

    @Test
    fun `sanitizeFileName should strip illegal filesystem characters`() {
        expectThat(
            DownloadPathUtil.sanitizeFileName("Ratatouille: A Tale?/Test*")
        ).isEqualTo("Ratatouille_ A Tale__Test_")
    }

    @Test
    fun `sanitizeFileName should fall back to untitled when input is blank after trimming`() {
        expectThat(DownloadPathUtil.sanitizeFileName("   ")).isEqualTo("untitled")
    }

    @Test
    fun `buildMediaFolderName should prefix sanitized title with media id`() {
        expectThat(DownloadPathUtil.buildMediaFolderName(testMovie))
            .isEqualTo("123-Everything Everywhere_ All at Once_")
    }

    @Test
    fun `buildEpisodeFolderName should format season and episode as sNNeNN`() {
        expectThat(DownloadPathUtil.buildEpisodeFolderName(testEpisode)).isEqualTo("s01e01")
    }

    @Test
    fun `buildEpisodeFolderName should zero pad double digit season and episode`() {
        val episode = testEpisode.copy(season = 12, number = 34)
        expectThat(DownloadPathUtil.buildEpisodeFolderName(episode)).isEqualTo("s12e34")
    }

    @Test
    fun `buildFileTitle should use episode title when present`() {
        expectThat(DownloadPathUtil.buildFileTitle(testMovie, testEpisode)).isEqualTo("Pilot")
    }

    @Test
    fun `buildFileTitle should fall back to media title when episode title is blank`() {
        val episode = testEpisode.copy(title = "")
        expectThat(DownloadPathUtil.buildFileTitle(testMovie, episode)).isEqualTo(testMovie.title)
    }

    @Test
    fun `buildFileTitle should fall back to media title when episode is null`() {
        expectThat(DownloadPathUtil.buildFileTitle(testMovie, null)).isEqualTo(testMovie.title)
    }

    @Test
    fun `buildStreamFileName should append extension to sanitized title`() {
        expectThat(DownloadPathUtil.buildStreamFileName("Pilot", "mp4")).isEqualTo("Pilot.mp4")
    }

    @Test
    fun `buildSubtitleFileName should append extension to sanitized title`() {
        expectThat(DownloadPathUtil.buildSubtitleFileName("Pilot", "srt")).isEqualTo("Pilot.srt")
    }
}
