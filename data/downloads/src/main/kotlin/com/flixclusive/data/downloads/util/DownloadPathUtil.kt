package com.flixclusive.data.downloads.util

object DownloadPathUtil {
    val STREAM_EXTENSIONS = setOf("mp4", "mkv", "mov", "webm")
    const val DEFAULT_STREAM_EXTENSION = "mp4"
    val SUBTITLE_EXTENSIONS = setOf("srt", "vtt", "ass", "ssa")
    const val DEFAULT_SUBTITLE_EXTENSION = "srt"

    private val illegalFileNameChars = Regex("[/\\\\:*?\"<>|]")

    fun sanitizeFileName(name: String): String {
        val sanitized = name.replace(illegalFileNameChars, "_").trim()
        return sanitized.ifBlank { "untitled" }
    }

    fun buildMediaFolderName(mediaId: String, mediaTitle: String): String = "$mediaId-${sanitizeFileName(mediaTitle)}"

    fun buildEpisodeFolderName(seasonNumber: Int, episodeNumber: Int): String = "s%02de%02d".format(
        seasonNumber,
        episodeNumber
    )

    fun buildFileTitle(mediaTitle: String, episodeTitle: String?): String =
        episodeTitle?.takeIf { it.isNotBlank() } ?: mediaTitle

    fun buildStreamFileName(fileTitle: String, extension: String): String =
        "${sanitizeFileName(fileTitle)}.$extension"

    fun buildSubtitleFileName(fileTitle: String, extension: String): String =
        "${sanitizeFileName(fileTitle)}.$extension"

    fun extensionFromUrl(url: String, fallback: String, allowed: Set<String>): String {
        val candidate = url
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('.', "")
            .lowercase()

        return candidate.takeIf { it in allowed } ?: fallback
    }
}
