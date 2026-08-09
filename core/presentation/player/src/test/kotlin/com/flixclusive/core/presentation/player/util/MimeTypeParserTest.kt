package com.flixclusive.core.presentation.player.util

import androidx.media3.common.MimeTypes
import com.flixclusive.core.presentation.player.model.track.PlayerSubtitle
import com.flixclusive.core.presentation.player.model.track.TrackSource
import com.flixclusive.core.presentation.player.util.MimeTypeParser.toMimeType
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MimeTypeParserTest {
    private fun subtitle(url: String, label: String = "English") = PlayerSubtitle(
        label = label,
        url = url,
        isDead = false,
        source = TrackSource.REMOTE,
    )

    @Test
    fun `toMimeType should never return null`() {
        // A null MIME here makes CustomSubtitleDecoderFactory.supportsFormat decline the track
        // outright, before CustomSubtitleParser ever gets a chance to sniff the real bytes.
        val result = subtitle("https://example.com/subtitle-with-no-known-extension").toMimeType()

        expectThat(result).isEqualTo(MimeTypes.APPLICATION_SUBRIP)
    }

    @Test
    fun `toMimeType should detect vtt from the url`() {
        expectThat(subtitle("https://example.com/sub.vtt").toMimeType()).isEqualTo(MimeTypes.TEXT_VTT)
    }

    @Test
    fun `toMimeType should detect ssa from the url`() {
        expectThat(subtitle("https://example.com/sub.ssa").toMimeType()).isEqualTo(MimeTypes.TEXT_SSA)
    }

    @Test
    fun `toMimeType should detect ttml from the url`() {
        expectThat(subtitle("https://example.com/sub.ttml").toMimeType()).isEqualTo(MimeTypes.APPLICATION_TTML)
    }

    @Test
    fun `toMimeType should fall back to SubRip for an unrecognized extension`() {
        expectThat(subtitle("https://example.com/sub.srt").toMimeType()).isEqualTo(MimeTypes.APPLICATION_SUBRIP)
        expectThat(subtitle("https://example.com/sub.txt").toMimeType()).isEqualTo(MimeTypes.APPLICATION_SUBRIP)
    }

    @Test
    fun `toMimeType should guess from the label rather than the url for a local content uri`() {
        val result = subtitle(url = "content://tree/document/msf%3A123", label = "English.vtt").toMimeType()

        expectThat(result).isEqualTo(MimeTypes.TEXT_VTT)
    }

    @Test
    fun `isM3U8 should detect m3u8 and m3u urls`() {
        expectThat(MimeTypeParser.isM3U8("https://example.com/master.m3u8")).isEqualTo(true)
        expectThat(MimeTypeParser.isM3U8("https://example.com/playlist.m3u")).isEqualTo(true)
    }

    @Test
    fun `isM3U8 should reject a plain mp4 url`() {
        expectThat(MimeTypeParser.isM3U8("https://example.com/movie.mp4")).isEqualTo(false)
    }
}
