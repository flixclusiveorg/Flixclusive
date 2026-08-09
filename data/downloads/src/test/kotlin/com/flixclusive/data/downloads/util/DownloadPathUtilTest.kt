package com.flixclusive.data.downloads.util

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DownloadPathUtilTest {
    private val mediaId = "123"
    private val mediaTitle = "Everything Everywhere: All at Once?"

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
        expectThat(DownloadPathUtil.buildMediaFolderName(mediaId, mediaTitle))
            .isEqualTo("123-Everything Everywhere_ All at Once_")
    }

    @Test
    fun `buildEpisodeFolderName should format season and episode as sNNeNN`() {
        expectThat(DownloadPathUtil.buildEpisodeFolderName(1, 1)).isEqualTo("s01e01")
    }

    @Test
    fun `buildEpisodeFolderName should zero pad double digit season and episode`() {
        expectThat(DownloadPathUtil.buildEpisodeFolderName(12, 34)).isEqualTo("s12e34")
    }

    @Test
    fun `buildFileName should append extension to sanitized title`() {
        expectThat(DownloadPathUtil.buildFileName("Pilot", "mp4")).isEqualTo("Pilot.mp4")
    }

    @Test
    fun `buildFileName should append a subtitle extension too`() {
        expectThat(DownloadPathUtil.buildFileName("Pilot", "srt")).isEqualTo("Pilot.srt")
    }

    @Test
    fun `extensionFromUrl should return the url extension when it is in the allowed set`() {
        val result = DownloadPathUtil.extensionFromUrl(
            url = "https://example.com/video.mkv?token=abc",
            fallback = "mp4",
            allowed = setOf("mp4", "mkv", "mov", "webm"),
        )

        expectThat(result).isEqualTo("mkv")
    }

    @Test
    fun `extensionFromUrl should fall back when the url has no recognizable extension`() {
        val result = DownloadPathUtil.extensionFromUrl(
            url = "https://example.com/stream/segment123",
            fallback = "mp4",
            allowed = setOf("mp4", "mkv", "mov", "webm"),
        )

        expectThat(result).isEqualTo("mp4")
    }
}
