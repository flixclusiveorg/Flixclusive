package com.flixclusive.core.ui.player.util

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
}
