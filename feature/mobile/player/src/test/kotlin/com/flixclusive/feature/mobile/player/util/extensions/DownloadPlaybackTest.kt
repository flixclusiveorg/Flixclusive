package com.flixclusive.feature.mobile.player.util.extensions

import androidx.media3.common.MimeTypes
import com.flixclusive.core.database.entity.downloads.DownloadItem
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.domain.downloads.usecase.CompletedDownloadFile
import com.flixclusive.domain.downloads.usecase.CompletedSubtitleFile
import com.flixclusive.model.media.common.MediaType
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class DownloadPlaybackTest {
    private fun downloadItem(
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        mediaType: MediaType = MediaType.MOVIE,
    ) = DownloadItem(
        ownerId = "owner",
        mediaId = "media-1",
        mediaTitle = "Example Movie",
        mediaType = mediaType,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
    )

    @Test
    fun `CompletedDownloadFile toPlayerServer should build a LOCAL server with no headers`() {
        val file = CompletedDownloadFile(uri = mockk(), mimeType = "video/mp4")

        val result = file.toPlayerServer(label = "Example Movie")

        expectThat(result.source).isEqualTo(TrackSource.LOCAL)
        expectThat(result.label).isEqualTo("Example Movie")
        expectThat(result.headers).isNull()
        expectThat(result.isDead).isEqualTo(false)
    }

    @Test
    fun `CompletedDownloadFile toPlayerSubtitles should map language to label and LOCAL source`() {
        val file = CompletedDownloadFile(
            uri = mockk(),
            mimeType = "video/mp4",
            subtitles = listOf(
                CompletedSubtitleFile(uri = mockk(), language = "English", extension = "srt"),
            ),
        )

        val result = file.toPlayerSubtitles()

        expectThat(result).isA<List<*>>()
        val subtitle = result.single()
        expectThat(subtitle.label).isEqualTo("English")
        expectThat(subtitle.source).isEqualTo(TrackSource.LOCAL)
    }

    @Test
    fun `toPlayerSubtitles should map each subtitle extension to its real mime type`() {
        val cases = mapOf(
            "srt" to MimeTypes.APPLICATION_SUBRIP,
            "vtt" to MimeTypes.TEXT_VTT,
            "ass" to MimeTypes.TEXT_SSA,
            "ssa" to MimeTypes.TEXT_SSA,
            "ttml" to MimeTypes.APPLICATION_TTML,
            "unknown" to MimeTypes.APPLICATION_SUBRIP,
        )

        cases.forEach { (extension, expectedMime) ->
            val file = CompletedDownloadFile(
                uri = mockk(),
                mimeType = "video/mp4",
                subtitles = listOf(CompletedSubtitleFile(uri = mockk(), language = "English", extension = extension)),
            )

            val result = file.toPlayerSubtitles().single()

            expectThat(result.mimeType).isEqualTo(expectedMime)
        }
    }

    @Test
    fun `DownloadItem toEpisode should build an episode from season and episode numbers`() {
        val result = downloadItem(seasonNumber = 1, episodeNumber = 3, mediaType = MediaType.SHOW).toEpisode()

        expectThat(result?.season).isEqualTo(1)
        expectThat(result?.number).isEqualTo(3)
        expectThat(result?.title).isEqualTo("S01E03")
    }

    @Test
    fun `DownloadItem toEpisode should return null for a movie download`() {
        val result = downloadItem().toEpisode()

        expectThat(result).isNull()
    }

    @Test
    fun `DownloadItem toFallbackMedia should carry the item's own type, id, and title`() {
        val item = downloadItem(mediaType = MediaType.SHOW)

        val result = item.toFallbackMedia()

        expectThat(result.type).isEqualTo(MediaType.SHOW)
        expectThat(result.id).isEqualTo(item.mediaId)
        expectThat(result.title).isEqualTo(item.mediaTitle)
        expectThat(result.posterImage).isNull()
    }
}
