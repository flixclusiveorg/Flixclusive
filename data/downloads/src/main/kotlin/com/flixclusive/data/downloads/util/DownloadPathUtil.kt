package com.flixclusive.data.downloads.util

import com.flixclusive.model.media.MediaMetadata
import com.flixclusive.model.media.common.tv.Episode

object DownloadPathUtil {
    private val illegalFileNameChars = Regex("[/\\\\:*?\"<>|]")

    fun sanitizeFileName(name: String): String {
        val sanitized = name.replace(illegalFileNameChars, "_").trim()
        return sanitized.ifBlank { "untitled" }
    }

    fun buildMediaFolderName(media: MediaMetadata): String = "${media.id}-${sanitizeFileName(media.title)}"

    fun buildEpisodeFolderName(episode: Episode): String = "s%02de%02d".format(episode.season, episode.number)

    fun buildFileTitle(media: MediaMetadata, episode: Episode?): String =
        episode?.title?.takeIf { it.isNotBlank() } ?: media.title

    fun buildStreamFileName(fileTitle: String, extension: String): String =
        "${sanitizeFileName(fileTitle)}.$extension"

    fun buildSubtitleFileName(fileTitle: String, extension: String): String =
        "${sanitizeFileName(fileTitle)}.$extension"
}
