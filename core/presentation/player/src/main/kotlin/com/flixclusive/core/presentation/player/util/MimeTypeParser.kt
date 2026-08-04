package com.flixclusive.core.presentation.player.util

import androidx.media3.common.MimeTypes
import com.flixclusive.core.presentation.player.model.track.PlayerSubtitle

internal object MimeTypeParser {
    /**
     * Checks if the given URL is a valid M3U8 file.
     *
     * @param url The file path or URL to parse.
     * @return The result of the check.
     */
    fun isM3U8(url: String): Boolean {
        return url.endsWith(".m3u8") ||
            url.contains(".m3u8") ||
            url.endsWith(".txt") ||
            url.contains(".txt") ||
            url.contains(".m3u") ||
            url.endsWith(".m3u")
    }

    /**
     * Guesses a subtitle's MIME type from its URL/label before any bytes are read. Always returns
     * a value in [com.flixclusive.core.presentation.player.renderer.CustomSubtitleDecoderFactory.supportedMimeTypes] —
     * a null return here made [com.flixclusive.core.presentation.player.renderer.CustomSubtitleDecoderFactory.supportsFormat]
     * decline the track outright, before [com.flixclusive.core.presentation.player.renderer.CustomSubtitleParser]
     * ever got a chance to sniff the real bytes and self-correct a wrong guess.
     */
    fun PlayerSubtitle.toMimeType(): String {
        val isLocalSubtitle = url.contains("content://")
        val uri = if (isLocalSubtitle) label else url

        return when {
            uri.endsWith("vtt", true) || uri.contains("vtt", true) -> MimeTypes.TEXT_VTT

            uri.endsWith("ssa", true) || uri.contains("ssa", true) -> MimeTypes.TEXT_SSA

            (uri.endsWith("ttml", true) || uri.contains("ttml", true)) ||
                (uri.endsWith("xml", true) || uri.contains("xml", true)) -> MimeTypes.APPLICATION_TTML

            // Also the default: srt is both the most common format and the download pipeline's
            // own fallback extension (DownloadPathUtil.DEFAULT_SUBTITLE_EXTENSION), so an
            // unrecognized guess is more likely SubRip than anything else.
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }
}
