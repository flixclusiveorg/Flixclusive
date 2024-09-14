package com.flixclusive.core.ui.player.util

import androidx.media3.common.MimeTypes
import com.flixclusive.model.provider.link.Subtitle

object MimeTypeParser {

    /**
     * Checks if the given URL is a valid M3U8 file.
     *
     * @param url The file path or URL to parse.
     * @return The result of the check.
     */
    fun isM3U8(url: String): Boolean {
        return url.endsWith(".m3u8")
            || url.contains(".m3u8")
            || url.endsWith(".txt")
            || url.contains(".txt")
            || url.contains(".m3u")
            || url.endsWith(".m3u")
    }

    fun Subtitle.toMimeType(): String {
        val isLocalSubtitle = url.contains("content://")
        val uri = if(isLocalSubtitle) {
            language
        } else url

        return when {
            uri.endsWith(".srt", true) || uri.contains(".srt", true) -> MimeTypes.APPLICATION_SUBRIP
            uri.endsWith(".vtt", true) || uri.contains(".vtt", true) -> MimeTypes.TEXT_VTT
            uri.endsWith(".ssa", true) || uri.contains(".ssa", true) -> MimeTypes.TEXT_SSA
            (uri.endsWith(".ttml", true) || uri.contains(".ttml", true)) || (uri.endsWith(".xml", true) || uri.contains(".xml", true)) -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.APPLICATION_SUBRIP
        }
    }
}
