package com.flixclusive.data.downloads.util

object HlsSignature {
    fun matchesUrl(url: String): Boolean {
        val path = url.substringBefore('#').substringBefore('?').lowercase()
        return path.endsWith(".m3u8") || path.endsWith(".m3u")
    }

    fun matchesContentType(contentType: String?): Boolean {
        val normalized = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return false
        return normalized in CONTENT_TYPES
    }

    fun matchesBody(
        bytes: ByteArray,
        length: Int = bytes.size,
    ): Boolean {
        val end = minOf(length, bytes.size)
        var start = 0
        if (end >= BOM.size && BOM.indices.all { bytes[it] == BOM[it] }) {
            start = BOM.size
        }

        while (start < end && bytes[start].toInt().toChar().isWhitespace()) {
            start++
        }

        if (end - start < PLAYLIST_HEADER.length) return false
        return PLAYLIST_HEADER.indices.all { bytes[start + it].toInt().toChar() == PLAYLIST_HEADER[it] }
    }

    private const val PLAYLIST_HEADER = "#EXTM3U"
    private val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private val CONTENT_TYPES = setOf(
        "application/vnd.apple.mpegurl",
        "application/x-mpegurl",
        "application/mpegurl",
        "audio/mpegurl",
        "audio/x-mpegurl",
        "vnd.apple.mpegurl",
    )
}
